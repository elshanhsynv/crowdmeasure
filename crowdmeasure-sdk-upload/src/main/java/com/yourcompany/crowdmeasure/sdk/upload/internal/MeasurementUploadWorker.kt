package com.crowdmeasure.sdk.upload.internal

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.crowdmeasure.sdk.upload.MeasurementUploadError
import com.crowdmeasure.sdk.upload.MeasurementUploadResult
import com.crowdmeasure.sdk.upload.UploadRun
import com.crowdmeasure.sdk.upload.UploadRunCode
import com.crowdmeasure.sdk.upload.UploadRunOutcome
import kotlinx.coroutines.flow.first

internal class MeasurementUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val config =
            UploadRuntime.get()?.config ?: com.crowdmeasure.sdk.upload.MeasurementUploadConfig()
        val store = UploadStore(applicationContext, config)
        if (!store.settings.first().enabled) {
            store.record(
                UploadRun(
                    System.currentTimeMillis(),
                    UploadRunOutcome.SKIPPED,
                    UploadRunCode.DISABLED,
                    0
                )
            )
            return Result.success()
        }
        val runtime = UploadRuntime.get() ?: run {
            store.record(
                UploadRun(
                    System.currentTimeMillis(),
                    UploadRunOutcome.FAILURE,
                    UploadRunCode.NOT_INSTALLED,
                    0
                )
            )
            return Result.failure(workDataOf(KEY_CODE to UploadRunCode.NOT_INSTALLED.name))
        }
        if (!UploadRuntime.mutex.tryLock()) {
            return Result.success()
        }
        return try {
            when (val result = executeUpload(runtime, store, runtime.config.defaultBatchSize)) {
                is MeasurementUploadResult.Success -> Result.success()
                is MeasurementUploadResult.Failure -> when (result.error) {
                    is MeasurementUploadError.TransientFailure -> Result.retry()
                    else -> Result.failure(workDataOf(KEY_CODE to result.error.toCode().name))
                }
            }
        } finally {
            UploadRuntime.mutex.unlock()
        }
    }

    companion object {
        const val KEY_CODE = "com.crowdmeasure.sdk.upload.code"
    }
}
