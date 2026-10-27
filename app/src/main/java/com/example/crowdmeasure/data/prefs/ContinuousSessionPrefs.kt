package com.example.crowdmeasure.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.crowdmeasure.domain.model.ArmedContinuousSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ContinuousSessionProgress(
    val endsAtUtcMs: Long?,
    val collectedCount: Int,
    val lastError: String?,
)

class ContinuousSessionPrefs(private val context: Context) {

    val progress: Flow<ContinuousSessionProgress> = context.dataStore.data.map { prefs ->
        ContinuousSessionProgress(
            endsAtUtcMs = prefs[DataStoreKeys.CONTINUOUS_ENDS_AT_UTC_MS],
            collectedCount = prefs[DataStoreKeys.CONTINUOUS_COLLECTED_COUNT] ?: 0,
            lastError = prefs[DataStoreKeys.CONTINUOUS_LAST_ERROR],
        )
    }

    val armed: Flow<ArmedContinuousSession?> = context.dataStore.data.map { prefs ->
        prefs.toArmedSession()
    }

    suspend fun armedSession(): ArmedContinuousSession? =
        context.dataStore.data.first().toArmedSession()

    suspend fun beginSession(endsAtUtcMs: Long) {
        context.dataStore.edit {
            it[DataStoreKeys.CONTINUOUS_ENDS_AT_UTC_MS] = endsAtUtcMs
            it[DataStoreKeys.CONTINUOUS_COLLECTED_COUNT] = 0
            it.remove(DataStoreKeys.CONTINUOUS_LAST_ERROR)
        }
    }

    suspend fun recordCollected() {
        context.dataStore.edit {
            it[DataStoreKeys.CONTINUOUS_COLLECTED_COUNT] =
                (it[DataStoreKeys.CONTINUOUS_COLLECTED_COUNT] ?: 0) + 1
            it.remove(DataStoreKeys.CONTINUOUS_LAST_ERROR)
        }
    }

    suspend fun recordError(message: String) {
        context.dataStore.edit { it[DataStoreKeys.CONTINUOUS_LAST_ERROR] = message }
    }

    suspend fun arm(durationMinutes: Int, gapSeconds: Int, afterMeasurementId: String?) {
        context.dataStore.edit {
            it[DataStoreKeys.CONTINUOUS_ARMED_DURATION_MINUTES] = durationMinutes
            it[DataStoreKeys.CONTINUOUS_ARMED_GAP_SECONDS] = gapSeconds
            // DataStore has no nullable strings; an empty id means "no measurement existed".
            it[DataStoreKeys.CONTINUOUS_ARMED_AFTER_MEASUREMENT_ID] = afterMeasurementId.orEmpty()
        }
    }

    suspend fun disarm() {
        context.dataStore.edit {
            it.remove(DataStoreKeys.CONTINUOUS_ARMED_DURATION_MINUTES)
            it.remove(DataStoreKeys.CONTINUOUS_ARMED_GAP_SECONDS)
            it.remove(DataStoreKeys.CONTINUOUS_ARMED_AFTER_MEASUREMENT_ID)
        }
    }

    private fun Preferences.toArmedSession(): ArmedContinuousSession? {
        val minutes = this[DataStoreKeys.CONTINUOUS_ARMED_DURATION_MINUTES] ?: return null
        val afterId = this[DataStoreKeys.CONTINUOUS_ARMED_AFTER_MEASUREMENT_ID]
        return ArmedContinuousSession(
            durationMinutes = minutes,
            gapSeconds = this[DataStoreKeys.CONTINUOUS_ARMED_GAP_SECONDS] ?: 0,
            afterMeasurementId = afterId?.takeIf { it.isNotEmpty() },
        )
    }
}
