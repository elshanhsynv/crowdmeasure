package com.example.crowdmeasure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val uploadWorkRepository: UploadWorkRepository,
    private val measurementRepository: MeasurementRepository,
    private val statusStore: WorkerStatusStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER_SOURCE) ?: TRIGGER_UNKNOWN
        markStartSafely()
        WorkerLog.i(TAG, "start attempt=$runAttemptCount trigger=$trigger")

        return try {
            val execution = uploadWorkRepository.execute(runAttemptCount)
            val statusResult = when (execution.outcome) {
                UploadExecution.Outcome.SUCCESS -> STATUS_SUCCESS
                UploadExecution.Outcome.RETRY -> STATUS_RETRY
                UploadExecution.Outcome.FAILURE -> STATUS_FAILURE
            }

            markEndSafely(statusResult, execution)

            when (execution.outcome) {
                UploadExecution.Outcome.SUCCESS -> {
                    WorkerLog.i(TAG, "completed code=${execution.code} uploaded=${execution.uploadedCount}")
                    Result.success()
                }

                UploadExecution.Outcome.RETRY -> {
                    WorkerLog.w(TAG, "retrying code=${execution.code}", execution.cause)
                    Result.retry()
                }

                UploadExecution.Outcome.FAILURE -> {
                    WorkerLog.e(TAG, "failing code=${execution.code}", execution.cause)
                    Result.failure(workDataOf(KEY_ERROR_CODE to execution.code))
                }
            }
        } catch (cancelled: CancellationException) {
            WorkerLog.w(TAG, "cancelled", cancelled)
            throw cancelled
        } catch (error: Exception) {
            val retry = WorkRetryClassifier.shouldRetryUpload(error, runAttemptCount)
            if (retry) {
                markUnexpectedEndSafely(result = STATUS_RETRY, error = error)
                WorkerLog.w(TAG, "retrying unexpected error", error)
                Result.retry()
            } else {
                markUnexpectedEndSafely(result = STATUS_FAILURE, error = error)
                WorkerLog.e(TAG, "failing unexpected error", error)
                Result.failure(workDataOf(KEY_ERROR_CODE to CODE_UNEXPECTED_ERROR))
            }
        }
    }

    private suspend fun markStartSafely() {
        try {
            statusStore.markUploadStart(System.currentTimeMillis())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            WorkerLog.w(TAG, "failed to persist upload start", error)
        }
    }

    private suspend fun markEndSafely(result: String, execution: UploadExecution) {
        try {
            statusStore.markUploadEnd(
                nowUtcMs = System.currentTimeMillis(),
                result = result,
                code = execution.code,
                uploadedCount = execution.uploadedCount,
                pendingCount = execution.pendingCount,
                failedCount = execution.failedCount,
                error = execution.cause?.toStatusError()
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            WorkerLog.w(TAG, "failed to persist upload end", error)
        }
    }

    private suspend fun markUnexpectedEndSafely(result: String, error: Throwable) {
        val counts = getQueueCountsSafely()
        try {
            statusStore.markUploadEnd(
                nowUtcMs = System.currentTimeMillis(),
                result = result,
                code = CODE_UNEXPECTED_ERROR,
                uploadedCount = 0,
                pendingCount = counts.first,
                failedCount = counts.second,
                error = error.toStatusError()
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (statusError: Exception) {
            WorkerLog.w(TAG, "failed to persist unexpected upload end", statusError)
        }
    }

    private suspend fun getQueueCountsSafely(): Pair<Int, Int> =
        try {
            measurementRepository.getPendingCount() to measurementRepository.getFailedCount()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            0 to 0
        }

    private fun Throwable.toStatusError(): String =
        "${this::class.java.simpleName}: ${message.orEmpty()}".take(160)

    companion object {
        private const val TAG = "UploadWorker"
        private const val CODE_UNEXPECTED_ERROR = "upload_unexpected_error"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_RETRY = "RETRY"
        private const val STATUS_FAILURE = "FAILURE"
        const val KEY_TRIGGER_SOURCE = "trigger_source"
        const val KEY_ERROR_CODE = "error_code"
        const val TRIGGER_PERIODIC = "periodic"
        const val TRIGGER_KICKOFF = "kickoff"
        const val TRIGGER_DEBUG = "debug"
        const val TRIGGER_UNKNOWN = "unknown"
    }
}
