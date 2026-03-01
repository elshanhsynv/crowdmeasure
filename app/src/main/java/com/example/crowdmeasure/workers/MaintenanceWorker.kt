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
class MaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val maintenanceWorkRepository: MaintenanceWorkRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        WorkerLog.i(TAG, "start attempt=$runAttemptCount")
        return try {
            val execution = maintenanceWorkRepository.execute(now, runAttemptCount)
            when (execution.outcome) {
                MaintenanceExecution.Outcome.SUCCESS -> Result.success()
                MaintenanceExecution.Outcome.RETRY -> {
                    WorkerLog.w(TAG, "cleanup retry code=${execution.code}", execution.cause)
                    Result.retry()
                }
                MaintenanceExecution.Outcome.FAILURE -> {
                    WorkerLog.e(TAG, "cleanup failed code=${execution.code}", execution.cause)
                    Result.failure(workDataOf(KEY_ERROR_CODE to execution.code))
                }
            }
        } catch (cancelled: CancellationException) {
            WorkerLog.w(TAG, "cancelled", cancelled)
            throw cancelled
        } catch (error: Exception) {
            val retry = WorkRetryClassifier.shouldRetry(error, runAttemptCount)
            if (retry) {
                WorkerLog.w(TAG, "cleanup retry on unexpected error", error)
                Result.retry()
            } else {
                WorkerLog.e(TAG, "cleanup failed on unexpected error", error)
                Result.failure(workDataOf(KEY_ERROR_CODE to CODE_UNEXPECTED_ERROR))
            }
        }
    }

    companion object {
        private const val TAG = "MaintenanceWorker"
        private const val CODE_UNEXPECTED_ERROR = "cleanup_unexpected_error"
        const val KEY_ERROR_CODE = "error_code"
    }
}
