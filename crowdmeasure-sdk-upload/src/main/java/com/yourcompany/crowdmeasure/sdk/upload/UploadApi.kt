package com.yourcompany.crowdmeasure.sdk.upload

import com.yourcompany.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.flow.Flow

data class MeasurementUploadItem(val measurement: Measurement, val installId: String)
data class UploadBatchResult(val uploadedIds: List<String>)

sealed interface MeasurementUploaderResult {
    data class Success(val result: UploadBatchResult) : MeasurementUploaderResult
    data class Failure(val error: MeasurementUploadError) : MeasurementUploaderResult
}

fun interface MeasurementUploader {
    suspend fun upload(items: List<MeasurementUploadItem>): MeasurementUploaderResult
}

fun interface InstallationIdProvider {
    suspend fun getInstallationId(): String
}

data class MeasurementUploadSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = CrowdMeasureUploads.DEFAULT_INTERVAL_MINUTES,
    val wifiOnly: Boolean = true,
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
sealed interface MeasurementUploadError {
    data object Disabled : MeasurementUploadError
    data object NotInstalled : MeasurementUploadError
    data class InvalidInterval(val intervalMinutes: Long) : MeasurementUploadError
    data class BackendRejected(val cause: Throwable? = null) : MeasurementUploadError
    data class TransientFailure(val cause: Throwable? = null) : MeasurementUploadError
    data class SerializationFailure(val cause: Throwable? = null) : MeasurementUploadError
    data class PersistenceFailure(val cause: Throwable? = null) : MeasurementUploadError
    data class SchedulingFailure(val cause: Throwable) : MeasurementUploadError
}

interface MeasurementUploadClient {
    suspend fun enable(intervalMinutes: Long = 60, wifiOnly: Boolean = true): MeasurementUploadResult<Unit>
    suspend fun disable(): MeasurementUploadResult<Unit>
    suspend fun uploadNow(limit: Int = 50): MeasurementUploadResult<Int>
    suspend fun enqueueUploadNow(): MeasurementUploadResult<Unit>
    suspend fun reschedule(): MeasurementUploadResult<Unit>
    fun observeQueue(): Flow<UploadQueueStatus>
    fun observeStatus(): Flow<MeasurementUploadStatus>
}
