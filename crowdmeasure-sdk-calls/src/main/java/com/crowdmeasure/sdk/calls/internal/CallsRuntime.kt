package com.crowdmeasure.sdk.calls.internal

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.DefaultDataMnoEligibility
import com.crowdmeasure.sdk.DefaultDataMnoEligibilityState
import com.crowdmeasure.sdk.calls.*
import kotlinx.coroutines.flow.map

internal class CallsSettingsStore(context: Context, preferencesName: String) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(preferencesName)
    }

    private object Keys {
        val cellular = booleanPreferencesKey("cellular_enabled")
        val voip = booleanPreferencesKey("voip_enabled")
        val missedAt = longPreferencesKey("missed_at")
        val missedCode = stringPreferencesKey("missed_code")
        val voipActive = booleanPreferencesKey("voip_active")
    }

    val settings = dataStore.data.map { preferences ->
        CallSamplingSettings(
            cellularEnabled = preferences[Keys.cellular]
                ?: CallSamplingSettings.DEFAULT_CELLULAR_ENABLED,
            voipEnabled = preferences[Keys.voip]
                ?: CallSamplingSettings.DEFAULT_VOIP_ENABLED,
        )
    }
    val missed = dataStore.data.map { p ->
        p[Keys.missedAt]?.let {
            MissedCallStart(
                it,
                runCatching { CallRunCode.valueOf(p[Keys.missedCode].orEmpty()) }.getOrDefault(
                    CallRunCode.UNEXPECTED_ERROR
                )
            )
        }
    }
    val voipActive = dataStore.data.map { it[Keys.voipActive] ?: false }
    suspend fun set(value: CallSamplingSettings) = dataStore.edit {
        it[Keys.cellular] = value.cellularEnabled; it[Keys.voip] =
        value.voipEnabled
    }

    suspend fun recordMissed(code: CallRunCode) = dataStore.edit {
        it[Keys.missedAt] = System.currentTimeMillis(); it[Keys.missedCode] = code.name
    }

    suspend fun clearMissed() = dataStore.edit {
        it.remove(Keys.missedAt)
        it.remove(Keys.missedCode)
    }

    suspend fun setVoipActive(active: Boolean) =
        dataStore.edit { it[Keys.voipActive] = active }
}

internal data class InstalledCallsRuntime(
    val context: Context,
    val sdk: CrowdMeasureSdk,
    val config: CallSamplingConfig,
    val store: CallStore,
    val settingsStore: CallsSettingsStore,
    val monitor: VoipCallMonitor,
    val sampler: CallSampler,
) {
    fun requirements(): CallSamplingRequirements =
        context.requirements(sdk.requirements.evaluateDefaultDataMno())
}

internal object CallsRuntime {
    @Volatile
    private var runtime: InstalledCallsRuntime? = null

    @Synchronized
    fun install(value: InstalledCallsRuntime) {
        val current = runtime
        if (current == null) {
            runtime = value
        } else if (current.sdk !== value.sdk || current.config != value.config) {
            throw IllegalStateException("CrowdMeasure calls runtime is already installed with a different configuration")
        }
    }

    fun get(): InstalledCallsRuntime? = runtime
}

internal fun Context.requirements(
    defaultDataMnoEligibility: DefaultDataMnoEligibility = DefaultDataMnoEligibility(),
): CallSamplingRequirements {
    fun granted(permission: String) = ContextCompat.checkSelfPermission(
        this,
        permission
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val location = getSystemService(android.location.LocationManager::class.java)
    val power = getSystemService(android.os.PowerManager::class.java)
    return CallSamplingRequirements(
        supportedAndroidVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        phoneStateGranted = granted(Manifest.permission.READ_PHONE_STATE),
        fineLocationGranted = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        backgroundLocationGranted = granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        locationServicesEnabled = location?.isLocationEnabled == true,
        notificationGranted = Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS),
        defaultDataMnoEligibility = defaultDataMnoEligibility,
//        batteryOptimizationIgnored = power?.isIgnoringBatteryOptimizations(packageName) == true,
    )
}

internal fun CallSamplingRequirements.failureCode() = when {
    !supportedAndroidVersion -> CallRunCode.UNSUPPORTED_ANDROID
    !phoneStateGranted -> CallRunCode.MISSING_PHONE_STATE
    !fineLocationGranted -> CallRunCode.MISSING_FINE_LOCATION
    !backgroundLocationGranted -> CallRunCode.MISSING_BACKGROUND_LOCATION
    !locationServicesEnabled -> CallRunCode.LOCATION_SERVICES_DISABLED
    !notificationGranted -> CallRunCode.MISSING_NOTIFICATIONS
    defaultDataMnoEligibility.state == DefaultDataMnoEligibilityState.MISMATCHED ->
        CallRunCode.TARGET_MNO_NOT_DEFAULT
    defaultDataMnoEligibility.state == DefaultDataMnoEligibilityState.UNAVAILABLE ->
        CallRunCode.TARGET_MNO_UNAVAILABLE
    else -> CallRunCode.OK
}
