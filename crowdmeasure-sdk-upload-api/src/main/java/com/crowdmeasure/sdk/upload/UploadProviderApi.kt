package com.crowdmeasure.sdk.upload

import com.crowdmeasure.sdk.model.Measurement

data class MeasurementUploadItem(
    val measurement: Measurement,
    val installId: String,
)

data class UploadBatchResult(
    val uploadedIds: Set<String> = emptySet(),
    val retryableIds: Set<String> = emptySet(),
    val rejectedIds: Set<String> = emptySet(),
) {
    init {
        require(uploadedIds.intersect(retryableIds).isEmpty()) { "uploadedIds and retryableIds overlap" }
        require(uploadedIds.intersect(rejectedIds).isEmpty()) { "uploadedIds and rejectedIds overlap" }
        require(retryableIds.intersect(rejectedIds).isEmpty()) { "retryableIds and rejectedIds overlap" }
    }
}

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
