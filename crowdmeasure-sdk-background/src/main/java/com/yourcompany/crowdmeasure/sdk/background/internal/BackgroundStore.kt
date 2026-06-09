package com.yourcompany.crowdmeasure.sdk.background.internal

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionSettings
import com.yourcompany.crowdmeasure.sdk.background.BackgroundRun
import com.yourcompany.crowdmeasure.sdk.background.BackgroundRunCode
import com.yourcompany.crowdmeasure.sdk.background.BackgroundRunOutcome
import com.yourcompany.crowdmeasure.sdk.background.CrowdMeasureBackground
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.backgroundDataStore by preferencesDataStore("crowdmeasure_sdk_background")

internal class BackgroundStore(private val context: Context) {
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val intervalMinutes = longPreferencesKey("interval_minutes")
        val wifiOnly = booleanPreferencesKey("wifi_only")
        val lastCompletedAt = longPreferencesKey("last_completed_at")
        val lastOutcome = stringPreferencesKey("last_outcome")
        val lastCode = stringPreferencesKey("last_code")
        val lastMeasurementId = stringPreferencesKey("last_measurement_id")
    }

    val settings: Flow<BackgroundCollectionSettings> = context.backgroundDataStore.data.map {
        BackgroundCollectionSettings(
            enabled = it[Keys.enabled] ?: false,
            intervalMinutes = it[Keys.intervalMinutes] ?: CrowdMeasureBackground.DEFAULT_INTERVAL_MINUTES,
            wifiOnly = it[Keys.wifiOnly] ?: false,
        )
    }

    val lastRun: Flow<BackgroundRun?> = context.backgroundDataStore.data.map {
        val completedAt = it[Keys.lastCompletedAt] ?: return@map null
        BackgroundRun(
            completedAtUtcMs = completedAt,
            outcome = enumValueOrDefault(it[Keys.lastOutcome], BackgroundRunOutcome.FAILURE),
            code = enumValueOrDefault(it[Keys.lastCode], BackgroundRunCode.UNEXPECTED_ERROR),
            measurementId = it[Keys.lastMeasurementId]?.takeIf(String::isNotBlank),
        )
    }

    suspend fun setSettings(settings: BackgroundCollectionSettings) {
        context.backgroundDataStore.edit {
            it[Keys.enabled] = settings.enabled
            it[Keys.intervalMinutes] = settings.intervalMinutes
            it[Keys.wifiOnly] = settings.wifiOnly
        }
    }

    suspend fun recordRun(run: BackgroundRun) {
        context.backgroundDataStore.edit {
            it[Keys.lastCompletedAt] = run.completedAtUtcMs
            it[Keys.lastOutcome] = run.outcome.name
            it[Keys.lastCode] = run.code.name
            it[Keys.lastMeasurementId] = run.measurementId.orEmpty()
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
