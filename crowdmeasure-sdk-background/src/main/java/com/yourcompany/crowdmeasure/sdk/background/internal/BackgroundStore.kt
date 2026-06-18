package com.crowdmeasure.sdk.background.internal

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.crowdmeasure.sdk.background.BackgroundConfig
import com.crowdmeasure.sdk.background.BackgroundCollectionSettings
import com.crowdmeasure.sdk.background.BackgroundRun
import com.crowdmeasure.sdk.background.BackgroundRunCode
import com.crowdmeasure.sdk.background.BackgroundRunOutcome
import com.crowdmeasure.sdk.background.CrowdMeasureBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class BackgroundStore(context: Context, private val config: BackgroundConfig) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(config.preferencesName)
    }

    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val intervalMinutes = longPreferencesKey("interval_minutes")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val lastCompletedAt = longPreferencesKey("last_completed_at")
        val lastOutcome = stringPreferencesKey("last_outcome")
        val lastCode = stringPreferencesKey("last_code")
        val lastMeasurementId = stringPreferencesKey("last_measurement_id")
    }

    val settings: Flow<BackgroundCollectionSettings> = dataStore.data.map { preferences ->
        BackgroundCollectionSettings(
            enabled = preferences[Keys.enabled] ?: config.defaultEnabled,
            intervalMinutes = preferences[Keys.intervalMinutes]
                ?: config.defaultIntervalMinutes,
            wifiOnly = preferences[Keys.wifiOnly]
                ?: config.defaultWifiOnly,
        )
    }

    val lastRun: Flow<BackgroundRun?> = dataStore.data.map {
        val completedAt = it[Keys.lastCompletedAt] ?: return@map null
        BackgroundRun(
            completedAtUtcMs = completedAt,
            outcome = enumValueOrDefault(it[Keys.lastOutcome], BackgroundRunOutcome.FAILURE),
            code = enumValueOrDefault(it[Keys.lastCode], BackgroundRunCode.UNEXPECTED_ERROR),
            measurementId = it[Keys.lastMeasurementId]?.takeIf(String::isNotBlank),
        )
    }

    suspend fun setSettings(settings: BackgroundCollectionSettings) {
        dataStore.edit {
            it[Keys.enabled] = settings.enabled
            it[Keys.intervalMinutes] = settings.intervalMinutes
            it[Keys.wifiOnly] = settings.wifiOnly
        }
    }

    suspend fun recordRun(run: BackgroundRun) {
        dataStore.edit {
            it[Keys.lastCompletedAt] = run.completedAtUtcMs
            it[Keys.lastOutcome] = run.outcome.name
            it[Keys.lastCode] = run.code.name
            it[Keys.lastMeasurementId] = run.measurementId.orEmpty()
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
