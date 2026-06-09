package com.yourcompany.crowdmeasure.sdk.calls.internal

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.calls.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import java.util.UUID

private val Context.callsDataStore by preferencesDataStore("crowdmeasure_sdk_calls")

internal class CallsSettingsStore(private val context: Context) {
    private object Keys {
        val cellular = booleanPreferencesKey("cellular_enabled")
        val voip = booleanPreferencesKey("voip_enabled")
        val uploads = booleanPreferencesKey("uploads_enabled")
        val interval = longPreferencesKey("upload_interval")
        val wifi = booleanPreferencesKey("upload_wifi")
        val missedAt = longPreferencesKey("missed_at")
        val missedCode = stringPreferencesKey("missed_code")
        val voipActive = booleanPreferencesKey("voip_active")
        val installId = stringPreferencesKey("install_id")
    }

    val settings = context.callsDataStore.data.map {
        CallSamplingSettings(
            it[Keys.cellular] ?: false,
            it[Keys.voip] ?: false,
            it[Keys.uploads] ?: false,
            it[Keys.interval] ?: 60,
            it[Keys.wifi] ?: true
        )
    }
    val missed = context.callsDataStore.data.map { p ->
        p[Keys.missedAt]?.let {
            MissedCallStart(
                it,
                runCatching { CallRunCode.valueOf(p[Keys.missedCode].orEmpty()) }.getOrDefault(
                    CallRunCode.UNEXPECTED_ERROR
                )
            )
        }
    }
    val voipActive = context.callsDataStore.data.map { it[Keys.voipActive] ?: false }
    suspend fun set(value: CallSamplingSettings) = context.callsDataStore.edit {
        it[Keys.cellular] = value.cellularEnabled; it[Keys.voip] =
        value.voipEnabled; it[Keys.uploads] = value.uploadsEnabled; it[Keys.interval] =
        value.uploadIntervalMinutes; it[Keys.wifi] = value.uploadWifiOnly
    }

    suspend fun recordMissed(code: CallRunCode) = context.callsDataStore.edit {
        it[Keys.missedAt] = System.currentTimeMillis(); it[Keys.missedCode] = code.name
    }

    suspend fun setVoipActive(active: Boolean) =
        context.callsDataStore.edit { it[Keys.voipActive] = active }

    suspend fun installationId(): String {
        var id = context.callsDataStore.data.first()[Keys.installId]
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString(); context.callsDataStore.edit {
                it[Keys.installId] = id
            }
        }
        return id
    }
}

internal data class InstalledCallsRuntime(
    val context: Context,
    val sdk: CrowdMeasureSdk,
    val config: CallSamplingConfig,
    val store: CallStore,
    val uploader: CallUploader?,
    val installationIdProvider: CallInstallationIdProvider,
    val settingsStore: CallsSettingsStore,
    val monitor: VoipCallMonitor,
)

internal object CallsRuntime {
    @Volatile
    private var runtime: InstalledCallsRuntime? = null
    val uploadMutex = Mutex()
    fun install(value: InstalledCallsRuntime) {
        runtime = value
    }

    fun get(): InstalledCallsRuntime? = runtime
}

internal fun Context.requirements(): CallSamplingRequirements {
    fun granted(permission: String) = androidx.core.content.ContextCompat.checkSelfPermission(
        this,
        permission
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val location = getSystemService(android.location.LocationManager::class.java)
    val power = getSystemService(android.os.PowerManager::class.java)
    return CallSamplingRequirements(
        supportedAndroidVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        phoneStateGranted = granted(android.Manifest.permission.READ_PHONE_STATE),
        fineLocationGranted = granted(android.Manifest.permission.ACCESS_FINE_LOCATION),
        backgroundLocationGranted = granted(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        locationServicesEnabled = location?.isLocationEnabled == true,
        notificationGranted = Build.VERSION.SDK_INT < 33 || granted(android.Manifest.permission.POST_NOTIFICATIONS),
        batteryOptimizationIgnored = power?.isIgnoringBatteryOptimizations(packageName) == true,
    )
}

internal fun CallSamplingRequirements.failureCode() = when {
    !supportedAndroidVersion -> CallRunCode.UNSUPPORTED_ANDROID
    !phoneStateGranted -> CallRunCode.MISSING_PHONE_STATE
    !fineLocationGranted -> CallRunCode.MISSING_FINE_LOCATION
    !backgroundLocationGranted -> CallRunCode.MISSING_BACKGROUND_LOCATION
    !locationServicesEnabled -> CallRunCode.LOCATION_SERVICES_DISABLED
    !notificationGranted -> CallRunCode.MISSING_NOTIFICATIONS
    else -> CallRunCode.OK
}
