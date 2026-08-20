package com.crowdmeasure.sdk.background.internal

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.background.BackgroundRun
import com.crowdmeasure.sdk.background.BackgroundRunCode
import com.crowdmeasure.sdk.background.BackgroundRunOutcome
import kotlinx.coroutines.flow.first

internal class MeasurementWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = BackgroundStore(applicationContext, BackgroundRuntime.config())
        val sdk = BackgroundRuntime.sdk() ?: return finish(
            store, BackgroundRunOutcome.FAILURE, BackgroundRunCode.NOT_INSTALLED, null
        )
        if (!BackgroundRuntime.runMutex.tryLock()) {
            return finish(
                store,
                BackgroundRunOutcome.SKIPPED,
                BackgroundRunCode.SKIPPED_CONCURRENT_RUN,
                null,
                false
            )
        }
        return try {
            val settings = store.settings.first()
            val latest = sdk.measurements.observeLatest().first()
            if (shouldSkipRecentRun(latest?.meta?.timestampUtcMs, System.currentTimeMillis(), settings.intervalMinutes)) {
                finish(
                    store,
                    BackgroundRunOutcome.SKIPPED,
                    BackgroundRunCode.SKIPPED_RECENT_RUN,
                    latest?.meta?.measurementId,
                    false
                )
            } else {
                when (val result = sdk.measurements.runAndSave()) {
                    is CrowdMeasureResult.Success -> finish(
                        store,
                        BackgroundRunOutcome.SUCCESS,
                        BackgroundRunCode.OK,
                        result.value.meta.measurementId,
                        false
                    )
                    is CrowdMeasureResult.Failure -> finish(
                        store,
                        BackgroundRunOutcome.FAILURE,
                        BackgroundRunCode.COLLECTION_FAILED,
                        null,
                        false
                    )
                }
            }
        } finally {
            BackgroundRuntime.runMutex.unlock()
        }
    }
}

internal class RetentionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = BackgroundStore(applicationContext, BackgroundRuntime.config())
        val sdk = BackgroundRuntime.sdk() ?: return finish(
            store, BackgroundRunOutcome.FAILURE, BackgroundRunCode.NOT_INSTALLED, null
        )
        return when (sdk.data.pruneExpiredMeasurements()) {
            is CrowdMeasureResult.Success -> Result.success()
            is CrowdMeasureResult.Failure -> finish(
                store, BackgroundRunOutcome.FAILURE, BackgroundRunCode.CLEANUP_FAILED, null
            )
        }
    }
}

private suspend fun finish(
    store: BackgroundStore,
    outcome: BackgroundRunOutcome,
    code: BackgroundRunCode,
    measurementId: String?,
    failWork: Boolean = true,
): ListenableWorker.Result {
    store.recordRun(BackgroundRun(System.currentTimeMillis(), outcome, code, measurementId))
    val output = workDataOf("com.crowdmeasure.sdk.background.code" to code.name)
    return if (failWork && outcome == BackgroundRunOutcome.FAILURE) {
        ListenableWorker.Result.failure(output)
    } else {
        ListenableWorker.Result.success(output)
    }
}
