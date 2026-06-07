package com.example.crowdmeasure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class CallUploadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: CallUploadWorkRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER_SOURCE) ?: TRIGGER_UNKNOWN
        WorkerLog.i(TAG, "start attempt=$runAttemptCount trigger=$trigger")

        return try {
            val execution = repository.execute(runAttemptCount)
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
            throw cancelled
        } catch (error: Exception) {
            if (WorkRetryClassifier.shouldRetryUpload(error, runAttemptCount)) {
                WorkerLog.w(TAG, "retrying unexpected error", error)
                Result.retry()
            } else {
                WorkerLog.e(TAG, "failing unexpected error", error)
                Result.failure(workDataOf(KEY_ERROR_CODE to CODE_UNEXPECTED_ERROR))
            }
        }
    }

    companion object {
        private const val TAG = "CallUploadWorker"
        private const val CODE_UNEXPECTED_ERROR = "call_upload_unexpected_error"
        const val KEY_TRIGGER_SOURCE = "trigger_source"
        const val KEY_ERROR_CODE = "error_code"
        const val TRIGGER_PERIODIC = "periodic"
        const val TRIGGER_CALL_ENDED = "call_ended"
        const val TRIGGER_UNKNOWN = "unknown"
    }
}
