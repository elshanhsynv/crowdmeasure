package com.yourcompany.crowdmeasure.sdk.upload.internal

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadError
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadResult
import com.yourcompany.crowdmeasure.sdk.upload.UploadRun
import com.yourcompany.crowdmeasure.sdk.upload.UploadRunCode
import com.yourcompany.crowdmeasure.sdk.upload.UploadRunOutcome
import kotlinx.coroutines.flow.first

internal class MeasurementUploadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val store = UploadStore(applicationContext)
        if (!store.settings.first().enabled) {
            store.record(UploadRun(System.currentTimeMillis(), UploadRunOutcome.SKIPPED, UploadRunCode.DISABLED, 0))
            return Result.success()
        }
        val runtime = UploadRuntime.get() ?: run {
            store.record(UploadRun(System.currentTimeMillis(), UploadRunOutcome.FAILURE, UploadRunCode.NOT_INSTALLED, 0))
            return Result.failure(workDataOf(KEY_CODE to UploadRunCode.NOT_INSTALLED.name))
        }
        if (!UploadRuntime.mutex.tryLock()) {
            return Result.success()
        }
        return try {
            when (val result = executeUpload(runtime, store, 50)) {
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
        const val KEY_CODE = "com.yourcompany.crowdmeasure.sdk.upload.code"
    }
}
