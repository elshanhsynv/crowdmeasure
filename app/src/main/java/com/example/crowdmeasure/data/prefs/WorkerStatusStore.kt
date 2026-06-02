package com.example.crowdmeasure.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerStatusStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    data class AutoRunStatus(
        val lastStartUtcMs: Long = 0L,
        val lastEndUtcMs: Long = 0L,
        val lastResult: String? = null,
        val lastCode: String? = null,
        val lastError: String? = null,
        val lastUploadedCount: Int = 0,
        val lastMeasurementId: String? = null,
        val lastMeasurementTimestampUtcMs: Long = 0L,
        val lastScheduleMinutes: Int = 0,
        val lastScheduleWifiOnly: Boolean = false,
        val lastSuccessUtcMs: Long = 0L,
    )

    data class UploadStatus(
        val lastStartUtcMs: Long = 0L,
        val lastEndUtcMs: Long = 0L,
        val lastResult: String? = null,
        val lastCode: String? = null,
        val uploadedCount: Int = 0,
        val pendingCount: Int = 0,
        val failedCount: Int = 0,
        val lastError: String? = null,
        val lastSuccessUtcMs: Long = 0L,
    )

    val autoRunStatus: Flow<AutoRunStatus> = context.dataStore.data.map { prefs ->
        AutoRunStatus(
            lastStartUtcMs = prefs[DataStoreKeys.AUTORUN_LAST_START_UTC_MS] ?: 0L,
            lastEndUtcMs = prefs[DataStoreKeys.AUTORUN_LAST_END_UTC_MS] ?: 0L,
            lastResult = prefs[DataStoreKeys.AUTORUN_LAST_RESULT],
            lastCode = prefs[DataStoreKeys.AUTORUN_LAST_CODE],
            lastError = prefs[DataStoreKeys.AUTORUN_LAST_ERROR],
            lastUploadedCount = prefs[DataStoreKeys.AUTORUN_LAST_UPLOADED_COUNT] ?: 0,
            lastMeasurementId = prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_ID],
            lastMeasurementTimestampUtcMs =
                prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_TIMESTAMP_UTC_MS] ?: 0L,
            lastScheduleMinutes = prefs[DataStoreKeys.AUTORUN_LAST_SCHEDULE_MINUTES] ?: 0,
            lastScheduleWifiOnly = prefs[DataStoreKeys.AUTORUN_LAST_SCHEDULE_WIFI_ONLY] ?: false,
            lastSuccessUtcMs = prefs[DataStoreKeys.AUTORUN_LAST_SUCCESS_UTC_MS] ?: 0L,
        )
    }

    val uploadStatus: Flow<UploadStatus> = context.dataStore.data.map { prefs ->
        UploadStatus(
            lastStartUtcMs = prefs[DataStoreKeys.UPLOAD_LAST_START_UTC_MS] ?: 0L,
            lastEndUtcMs = prefs[DataStoreKeys.UPLOAD_LAST_END_UTC_MS] ?: 0L,
            lastResult = prefs[DataStoreKeys.UPLOAD_LAST_RESULT],
            lastCode = prefs[DataStoreKeys.UPLOAD_LAST_CODE],
            uploadedCount = prefs[DataStoreKeys.UPLOAD_LAST_UPLOADED_COUNT] ?: 0,
            pendingCount = prefs[DataStoreKeys.UPLOAD_PENDING_COUNT] ?: 0,
            failedCount = prefs[DataStoreKeys.UPLOAD_FAILED_COUNT] ?: 0,
            lastError = prefs[DataStoreKeys.UPLOAD_LAST_ERROR],
            lastSuccessUtcMs = prefs[DataStoreKeys.UPLOAD_LAST_SUCCESS_UTC_MS] ?: 0L,
        )
    }

    suspend fun markAutoRunStart(nowUtcMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.AUTORUN_LAST_START_UTC_MS] = nowUtcMs
            prefs[DataStoreKeys.AUTORUN_LAST_UPLOADED_COUNT] = 0
            prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_ID] = ""
            prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_TIMESTAMP_UTC_MS] = 0L
        }
    }

    suspend fun markAutoRunEnd(
        nowUtcMs: Long,
        result: String,
        code: String,
        uploadedCount: Int,
        measurementId: String?,
        measurementTimestampUtcMs: Long
    ) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.AUTORUN_LAST_END_UTC_MS] = nowUtcMs
            prefs[DataStoreKeys.AUTORUN_LAST_RESULT] = result
            prefs[DataStoreKeys.AUTORUN_LAST_CODE] = code
            prefs[DataStoreKeys.AUTORUN_LAST_ERROR] = ""
            prefs[DataStoreKeys.AUTORUN_LAST_UPLOADED_COUNT] = uploadedCount
            prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_ID] = (measurementId ?: "")
            prefs[DataStoreKeys.AUTORUN_LAST_MEASUREMENT_TIMESTAMP_UTC_MS] =
                measurementTimestampUtcMs
        }
    }

    suspend fun markUploadStart(nowUtcMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.UPLOAD_LAST_START_UTC_MS] = nowUtcMs
            prefs[DataStoreKeys.UPLOAD_LAST_ERROR] = ""
        }
    }

    suspend fun markUploadEnd(
        nowUtcMs: Long,
        result: String,
        code: String,
        uploadedCount: Int,
        pendingCount: Int,
        failedCount: Int,
        error: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.UPLOAD_LAST_END_UTC_MS] = nowUtcMs
            prefs[DataStoreKeys.UPLOAD_LAST_RESULT] = result
            prefs[DataStoreKeys.UPLOAD_LAST_CODE] = code
            prefs[DataStoreKeys.UPLOAD_LAST_UPLOADED_COUNT] = uploadedCount
            prefs[DataStoreKeys.UPLOAD_PENDING_COUNT] = pendingCount
            prefs[DataStoreKeys.UPLOAD_FAILED_COUNT] = failedCount
            prefs[DataStoreKeys.UPLOAD_LAST_ERROR] = error ?: ""
            if (result == "SUCCESS" && code == "upload_ok") {
                prefs[DataStoreKeys.UPLOAD_LAST_SUCCESS_UTC_MS] = nowUtcMs
            }
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
