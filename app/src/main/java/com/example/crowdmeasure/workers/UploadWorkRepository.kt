package com.example.crowdmeasure.workers

import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UploadRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class UploadExecution(
    val outcome: Outcome,
    val code: String,
    val uploadedCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val cause: Throwable? = null
) {
    enum class Outcome { SUCCESS, RETRY, FAILURE }
}

class UploadWorkRepository @Inject constructor(
    private val sessionRepo: UserSessionRepository,
    private val measurementRepo: MeasurementRepository,
    private val uploadRepo: UploadRepository
) {
    suspend fun execute(runAttemptCount: Int): UploadExecution {
        val settings = sessionRepo.settings.first()
        if (!settings.firestoreUploadsEnabled) {
            return UploadExecution(
                outcome = UploadExecution.Outcome.SUCCESS,
                code = CODE_GATE_BLOCKED,
                pendingCount = measurementRepo.getPendingCount(),
                failedCount = measurementRepo.getFailedCount()
            )
        }

        return try {
            val uploadedCount = uploadRepo.uploadPending(limit = UPLOAD_LIMIT).getOrThrow()
            UploadExecution(
                outcome = UploadExecution.Outcome.SUCCESS,
                code = CODE_OK,
                uploadedCount = uploadedCount,
                pendingCount = measurementRepo.getPendingCount(),
                failedCount = measurementRepo.getFailedCount()
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val retry = WorkRetryClassifier.shouldRetryUpload(error, runAttemptCount)
            UploadExecution(
                outcome = if (retry) UploadExecution.Outcome.RETRY else UploadExecution.Outcome.FAILURE,
                code = CODE_UPLOAD_FAILED,
                pendingCount = measurementRepo.getPendingCount(),
                failedCount = measurementRepo.getFailedCount(),
                cause = error
            )
        }
    }

    companion object {
        const val CODE_OK = "upload_ok"
        const val CODE_GATE_BLOCKED = "upload_gate_blocked"
        const val CODE_UPLOAD_FAILED = "upload_failed"
        private const val UPLOAD_LIMIT = 50
    }
}
