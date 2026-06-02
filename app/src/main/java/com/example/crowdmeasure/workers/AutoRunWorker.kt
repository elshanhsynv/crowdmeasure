package com.example.crowdmeasure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class AutoRunWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val autoRunWorkRepository: AutoRunWorkRepository,
    private val statusStore: WorkerStatusStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val trigger = inputData.getString(KEY_TRIGGER_SOURCE) ?: TRIGGER_UNKNOWN

        try {
            statusStore.markAutoRunStart(now)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            WorkerLog.w(TAG, "failed to persist worker start", error)
        }
        WorkerLog.i(TAG, "start attempt=$runAttemptCount trigger=$trigger")

        return try {
            val execution = autoRunWorkRepository.execute(
                nowUtcMs = now, runAttemptCount = runAttemptCount
            )

            val statusResult = when (execution.outcome) {
                AutoRunExecution.Outcome.SUCCESS -> STATUS_SUCCESS
                AutoRunExecution.Outcome.RETRY -> STATUS_RETRY
                AutoRunExecution.Outcome.FAILURE -> STATUS_FAILURE
            }

            markEndSafely(
                result = statusResult,
                code = execution.code,
                uploadedCount = execution.uploadedCount,
                measurementId = execution.measurementId,
                measurementTimestampUtcMs = execution.measurementTimestampUtcMs
            )

            when (execution.outcome) {
                AutoRunExecution.Outcome.SUCCESS -> {
                    if (execution.code != AutoRunWorkRepository.CODE_OK) {
                        WorkerLog.i(TAG, "completed with non-fatal code=${execution.code}")
                    }
                    Result.success()
                }

                AutoRunExecution.Outcome.RETRY -> {
                    WorkerLog.w(TAG, "retrying code=${execution.code}", execution.cause)
                    Result.retry()
                }

                AutoRunExecution.Outcome.FAILURE -> {
                    WorkerLog.e(TAG, "failing code=${execution.code}", execution.cause)
                    Result.failure(workDataOf(KEY_ERROR_CODE to execution.code))
                }
            }
        } catch (cancelled: CancellationException) {
            WorkerLog.w(TAG, "cancelled", cancelled)
            throw cancelled
        } catch (error: Exception) {
            val retry = WorkRetryClassifier.shouldRetry(error, runAttemptCount)
            val resultCode = CODE_UNEXPECTED_ERROR
            markEndSafely(
                result = if (retry) STATUS_RETRY else STATUS_FAILURE,
                code = resultCode,
                uploadedCount = 0,
                measurementId = null,
                measurementTimestampUtcMs = 0L
            )
            if (retry) {
                WorkerLog.w(TAG, "retrying unexpected error", error)
                Result.retry()
            } else {
                WorkerLog.e(TAG, "failing unexpected error", error)
                Result.failure(workDataOf(KEY_ERROR_CODE to resultCode))
            }
        }
    }

    private suspend fun markEndSafely(
        result: String,
        code: String,
        uploadedCount: Int,
        measurementId: String?,
        measurementTimestampUtcMs: Long
    ) {
        try {
            statusStore.markAutoRunEnd(
                nowUtcMs = System.currentTimeMillis(),
                result = result,
                code = code,
                uploadedCount = uploadedCount,
                measurementId = measurementId,
                measurementTimestampUtcMs = measurementTimestampUtcMs
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            WorkerLog.w(TAG, "failed to persist worker end", error)
        }
    }

    companion object {
        private const val TAG = "AutoRunWorker"
        private const val STATUS_SUCCESS = "SUCCESS"
        private const val STATUS_RETRY = "RETRY"
        private const val STATUS_FAILURE = "FAILURE"
        const val KEY_TRIGGER_SOURCE = "trigger_source"
        const val KEY_ERROR_CODE = "error_code"
        const val TRIGGER_PERIODIC = "periodic"
        const val TRIGGER_KICKOFF = "kickoff"
        const val TRIGGER_DEBUG = "debug"
        const val TRIGGER_UNKNOWN = "unknown"
        private const val CODE_UNEXPECTED_ERROR = "unexpected_error"
    }
}
