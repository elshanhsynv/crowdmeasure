package com.crowdmeasure.sdk.upload

import kotlinx.coroutines.flow.Flow

data class MeasurementUploadConfig(
    val preferencesName: String = "crowdmeasure_sdk_upload",
    val defaultBatchSize: Int = DEFAULT_BATCH_SIZE,
    val defaultIntervalMinutes: Long = CrowdMeasureUploads.DEFAULT_INTERVAL_MINUTES,
    val defaultWifiOnly: Boolean = CrowdMeasureUploads.DEFAULT_WIFI_ONLY,
    val defaultMeasurementUploadEnabled: Boolean = DEFAULT_MEASUREMENT_UPLOAD_ENABLED,
) {
    init {
        require(preferencesName.isNotBlank()) { "preferencesName must not be blank" }
        require(defaultBatchSize in 1..1_000) { "defaultBatchSize must be between 1 and 1000" }
        require(defaultIntervalMinutes in CrowdMeasureUploads.MIN_INTERVAL_MINUTES..CrowdMeasureUploads.MAX_INTERVAL_MINUTES) {
            "defaultIntervalMinutes must be between 20 minutes and 7 days"
        }
    }

    companion object {
        const val DEFAULT_BATCH_SIZE = 50
        const val DEFAULT_MEASUREMENT_UPLOAD_ENABLED = true
    }
}

data class MeasurementUploadSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Long = CrowdMeasureUploads.DEFAULT_INTERVAL_MINUTES,
    val wifiOnly: Boolean = false,
)

data class UploadQueueStatus(val pendingCount: Int, val failedCount: Int)
data class MeasurementUploadStatus(
    val settings: MeasurementUploadSettings,
    val workState: UploadWorkState,
    val queue: UploadQueueStatus,
    val lastRun: UploadRun?,
)

data class UploadRun(
    val completedAtUtcMs: Long,
    val outcome: UploadRunOutcome,
    val code: UploadRunCode,
    val uploadedCount: Int,
    val errorMessage: String? = null,
)

enum class UploadWorkState { DISABLED, ENQUEUED, RUNNING, BLOCKED, SUCCEEDED, FAILED, CANCELLED, UNKNOWN }
enum class UploadRunOutcome { SUCCESS, SKIPPED, RETRYING, FAILURE }
enum class UploadRunCode { OK, NOTHING_TO_UPLOAD, DISABLED, NOT_INSTALLED, BACKEND_REJECTED, TRANSIENT_FAILURE, SERIALIZATION_FAILED, PERSISTENCE_FAILED, UNEXPECTED_ERROR }

sealed interface MeasurementUploadResult<out T> {
    data class Success<T>(val value: T) : MeasurementUploadResult<T>
    data class Failure(val error: MeasurementUploadError) : MeasurementUploadResult<Nothing>
}

interface MeasurementUploadClient {
    suspend fun enable(
        intervalMinutes: Long = CrowdMeasureUploads.DEFAULT_INTERVAL_MINUTES,
        wifiOnly: Boolean = CrowdMeasureUploads.DEFAULT_WIFI_ONLY
    ): MeasurementUploadResult<Unit>

    suspend fun disable(): MeasurementUploadResult<Unit>
    suspend fun uploadNow(limit: Int = 50): MeasurementUploadResult<Int>
    suspend fun enqueueUploadNow(): MeasurementUploadResult<Unit>
    suspend fun reschedule(): MeasurementUploadResult<Unit>
    fun observeQueue(): Flow<UploadQueueStatus>
    fun observeStatus(): Flow<MeasurementUploadStatus>
}
