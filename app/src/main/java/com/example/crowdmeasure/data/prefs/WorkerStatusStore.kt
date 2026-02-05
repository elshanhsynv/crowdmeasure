package com.example.crowdmeasure.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerStatusStore @Inject constructor(
    private val context: Context
) {
    data class AutoRunStatus(
        val lastStartUtcMs: Long = 0L,
        val lastEndUtcMs: Long = 0L,
        val lastResult: String? = null,
        val lastError: String? = null,
        val lastUploadedCount: Int = 0,
        val lastMeasurementId: String? = null,
        val lastScheduleMinutes: Int = 0,
        val lastScheduleWifiOnly: Boolean = false,
        val lastSuccessUtcMs: Long = 0L,
    )

    val autoRunStatus: Flow<AutoRunStatus> = context.dataStore.data.map { prefs ->
        AutoRunStatus(
            lastStartUtcMs = prefs[DataStoreKeys.AUTORUN_LAST_START_UTC_MS] ?: 0L,
            lastEndUtcMs = prefs[DataStoreKeys.AUTORUN_LAST_END_UTC_MS] ?: 0L,
            lastResult = prefs[DataStoreKeys.AUTORUN_LAST_RESULT],
            lastError = prefs[DataStoreKeys.AUTORUN_LAST_ERROR],
            lastUploadedCount = prefs[DataStoreKeys.AUTORUN_LAST_UPLOADED_COUNT] ?: 0,
            lastMeasurementId = prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_ID],
            lastScheduleMinutes = prefs[DataStoreKeys.AUTORUN_LAST_SCHEDULE_MINUTES] ?: 0,
            lastScheduleWifiOnly = prefs[DataStoreKeys.AUTORUN_LAST_SCHEDULE_WIFI_ONLY] ?: false,
            lastSuccessUtcMs = prefs[DataStoreKeys.AUTORUN_LAST_SUCCESS_UTC_MS] ?: 0L,
        )
    }

    suspend fun markAutoRunStart(nowUtcMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.AUTORUN_LAST_START_UTC_MS] = nowUtcMs
            prefs[DataStoreKeys.AUTORUN_LAST_ERROR] = ""
            prefs[DataStoreKeys.AUTORUN_LAST_UPLOADED_COUNT] = 0
            prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_ID] = ""
        }
    }

    suspend fun markAutoRunEnd(
        nowUtcMs: Long,
        result: String,
        error: String?,
        uploadedCount: Int,
        measurementId: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.AUTORUN_LAST_END_UTC_MS] = nowUtcMs
            prefs[DataStoreKeys.AUTORUN_LAST_RESULT] = result
            prefs[DataStoreKeys.AUTORUN_LAST_ERROR] = (error ?: "")
            prefs[DataStoreKeys.AUTORUN_LAST_UPLOADED_COUNT] = uploadedCount
            prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_ID] = (measurementId ?: "")
        }
    }

    suspend fun rememberSchedule(minutes: Int, wifiOnly: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.AUTORUN_LAST_SCHEDULE_MINUTES] = minutes
            prefs[DataStoreKeys.AUTORUN_LAST_SCHEDULE_WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun getLastSuccessUtcMs(): Long =
        context.dataStore.data.first()[DataStoreKeys.AUTORUN_LAST_SUCCESS_UTC_MS] ?: 0L

    suspend fun setLastSuccessUtcMs(value: Long) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.AUTORUN_LAST_SUCCESS_UTC_MS] = value
        }
    }
}
