package com.crowdmeasure.sdk.upload.internal

import android.content.Context
import androidx.work.*
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.upload.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

internal object UploadWorkNames {
    const val PREFIX = "com.crowdmeasure.sdk.upload"
    const val PERIODIC = "$PREFIX.periodic"
    const val IMMEDIATE = "$PREFIX.immediate"
    const val TAG = "$PREFIX.owned"
}

internal class MeasurementUploadClientImpl(
    context: Context,
    private val sdk: CrowdMeasureSdk,
    private val config: MeasurementUploadConfig,
) : MeasurementUploadClient {
    private val store = UploadStore(context, config)
    private val workManager = WorkManager.getInstance(context)

    override suspend fun enable(
        intervalMinutes: Long,
        wifiOnly: Boolean
    ): MeasurementUploadResult<Unit> {
        if (intervalMinutes !in CrowdMeasureUploads.MIN_INTERVAL_MINUTES..CrowdMeasureUploads.MAX_INTERVAL_MINUTES) {
            return MeasurementUploadResult.Failure(
                MeasurementUploadError.InvalidInterval(
                    intervalMinutes
                )
            )
        }
        return scheduling {
            val settings = MeasurementUploadSettings(true, intervalMinutes, wifiOnly)
            schedule(settings)
            store.setSettings(settings)
        }
    }

    override suspend fun disable(): MeasurementUploadResult<Unit> = scheduling {
        cancel()
        store.setSettings(store.settings.first().copy(enabled = false))
    }

    override suspend fun uploadNow(limit: Int): MeasurementUploadResult<Int> {
        if (!store.settings.first().enabled) {
            return MeasurementUploadResult.Failure(MeasurementUploadError.Disabled)
        }
        val runtime = UploadRuntime.get()
            ?: return MeasurementUploadResult.Failure(MeasurementUploadError.NotInstalled)
        return UploadRuntime.mutex.withLock {
            executeUpload(runtime, store, limit.coerceIn(1, 1_000))
        }
    }

    override suspend fun enqueueUploadNow(): MeasurementUploadResult<Unit> {
        val settings = store.settings.first()
        if (!settings.enabled) return MeasurementUploadResult.Failure(MeasurementUploadError.Disabled)
        return scheduling {
            val request = OneTimeWorkRequestBuilder<MeasurementUploadWorker>()
                .setConstraints(constraints(settings.wifiOnly, false))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(UploadWorkNames.TAG)
                .build()
            workManager.enqueueUniqueWork(
                UploadWorkNames.IMMEDIATE,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun reschedule(): MeasurementUploadResult<Unit> = scheduling {
        val settings = store.settings.first()
        if (settings.enabled) schedule(settings) else cancel()
    }

    override fun observeQueue() = sdk.queue.observeStatus().map {
        UploadQueueStatus(it.pendingCount, it.failedCount)
    }

    override fun observeStatus() = combine(
        store.settings,
        store.lastRun,
        observeQueue(),
        workManager.getWorkInfosForUniqueWorkFlow(UploadWorkNames.PERIODIC),
    ) { settings, lastRun, queue, work ->
        MeasurementUploadStatus(
            settings,
            if (!settings.enabled) UploadWorkState.DISABLED else mapState(work.firstOrNull()?.state),
            queue,
            lastRun,
        )
    }

    private fun schedule(settings: MeasurementUploadSettings) {
        val flex = (settings.intervalMinutes / 3).coerceAtLeast(5)
        val request = PeriodicWorkRequestBuilder<MeasurementUploadWorker>(
            settings.intervalMinutes, TimeUnit.MINUTES, flex, TimeUnit.MINUTES
        )
            .setConstraints(constraints(settings.wifiOnly, true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(UploadWorkNames.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UploadWorkNames.PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancel() {
        workManager.cancelUniqueWork(UploadWorkNames.PERIODIC)
        workManager.cancelUniqueWork(UploadWorkNames.IMMEDIATE)
    }

    private fun constraints(wifiOnly: Boolean, batteryNotLow: Boolean) = Constraints.Builder()
        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(batteryNotLow)
        .build()

    private inline fun scheduling(block: () -> Unit): MeasurementUploadResult<Unit> =
        runCatching(block).fold(
            { MeasurementUploadResult.Success(Unit) },
            { MeasurementUploadResult.Failure(MeasurementUploadError.SchedulingFailure(it)) },
        )

    private fun mapState(state: WorkInfo.State?): UploadWorkState =
        runCatching { state?.name?.let(UploadWorkState::valueOf) }.getOrNull()
            ?: UploadWorkState.UNKNOWN
}

internal suspend fun executeUpload(
    runtime: InstalledUploadRuntime,
    store: UploadStore,
    limit: Int,
): MeasurementUploadResult<Int> {
    val candidates = try {
        runtime.sdk.queue.getCandidates(limit)
    } catch (error: Exception) {
        return finishFailure(
            store,
            UploadRunCode.PERSISTENCE_FAILED,
            MeasurementUploadError.PersistenceFailure(error)
        )
    }
    if (candidates.isEmpty()) {
        store.record(
            UploadRun(
                System.currentTimeMillis(),
                UploadRunOutcome.SKIPPED,
                UploadRunCode.NOTHING_TO_UPLOAD,
                0
            )
        )
        return MeasurementUploadResult.Success(0)
    }
    val installId = try {
        runtime.installationIdProvider.getInstallationId()
    } catch (error: Exception) {
        return finishFailure(
            store,
            UploadRunCode.PERSISTENCE_FAILED,
            MeasurementUploadError.PersistenceFailure(error)
        )
    }
    val items = candidates.map { MeasurementUploadItem(it, installId) }
    return when (val result = runtime.uploader.upload(items)) {
        is MeasurementUploaderResult.Success -> {
            val uploadedIds = result.result.uploadedIds.toList()
            val rejectedIds = result.result.rejectedIds.toList()
            val knownIds = candidates.map { it.meta.measurementId }.toSet()
            val returnedIds =
                result.result.uploadedIds + result.result.retryableIds + result.result.rejectedIds
            if (!knownIds.containsAll(returnedIds)) {
                return finishFailure(
                    store,
                    UploadRunCode.BACKEND_REJECTED,
                    MeasurementUploadError.BackendRejected(
                        IllegalArgumentException("Uploader returned IDs outside the requested batch")
                    ),
                )
            }
            when (val persisted = runtime.sdk.queue.markUploaded(uploadedIds)) {
                is CrowdMeasureResult.Success -> {
                    if (rejectedIds.isNotEmpty()) runtime.sdk.queue.markFailed(rejectedIds)
                    val outcome =
                        if (result.result.retryableIds.isEmpty()) UploadRunOutcome.SUCCESS else UploadRunOutcome.RETRYING
                    store.record(
                        UploadRun(
                            System.currentTimeMillis(),
                            outcome,
                            UploadRunCode.OK,
                            uploadedIds.size
                        )
                    )
                    MeasurementUploadResult.Success(uploadedIds.size)
                }

                is CrowdMeasureResult.Failure -> finishFailure(
                    store,
                    UploadRunCode.PERSISTENCE_FAILED,
                    MeasurementUploadError.PersistenceFailure()
                )
            }
        }

        is MeasurementUploaderResult.Failure -> {
            val ids = candidates.map { it.meta.measurementId }
            if (result.error !is MeasurementUploadError.TransientFailure) {
                runtime.sdk.queue.markFailed(ids)
            }
            val code = result.error.toCode()
            finishFailure(store, code, result.error)
        }
    }
}

private suspend fun finishFailure(
    store: UploadStore,
    code: UploadRunCode,
    error: MeasurementUploadError,
): MeasurementUploadResult.Failure {
    val outcome =
        if (error is MeasurementUploadError.TransientFailure) UploadRunOutcome.RETRYING else UploadRunOutcome.FAILURE
    store.record(
        UploadRun(
            System.currentTimeMillis(),
            outcome,
            code,
            0,
            error.toString().take(160)
        )
    )
    return MeasurementUploadResult.Failure(error)
}

internal fun MeasurementUploadError.toCode(): UploadRunCode = when (this) {
    MeasurementUploadError.Disabled -> UploadRunCode.DISABLED
    MeasurementUploadError.NotInstalled -> UploadRunCode.NOT_INSTALLED
    is MeasurementUploadError.BackendRejected -> UploadRunCode.BACKEND_REJECTED
    is MeasurementUploadError.TransientFailure -> UploadRunCode.TRANSIENT_FAILURE
    is MeasurementUploadError.SerializationFailure -> UploadRunCode.SERIALIZATION_FAILED
    is MeasurementUploadError.PersistenceFailure -> UploadRunCode.PERSISTENCE_FAILED
    is MeasurementUploadError.InvalidInterval, is MeasurementUploadError.SchedulingFailure -> UploadRunCode.UNEXPECTED_ERROR
}
