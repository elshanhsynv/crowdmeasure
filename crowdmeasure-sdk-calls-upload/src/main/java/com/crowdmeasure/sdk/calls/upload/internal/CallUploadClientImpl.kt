package com.crowdmeasure.sdk.calls.upload.internal

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.*
import com.crowdmeasure.sdk.calls.*
import com.crowdmeasure.sdk.calls.upload.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit
import java.util.UUID

internal object CallUploadWorkNames {
    const val PERIODIC = "com.crowdmeasure.sdk.calls.upload.periodic"
    const val IMMEDIATE = "com.crowdmeasure.sdk.calls.upload.immediate"
}

internal class CallUploadClientImpl(context: Context, private val config: CallUploadConfig) : CallUploadClient {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        appContext.preferencesDataStoreFile(config.preferencesName)
    }
    private object Keys {
        val enabled = booleanPreferencesKey("enabled")
        val interval = longPreferencesKey("interval")
        val wifi = booleanPreferencesKey("wifi_only")
        val installationId = stringPreferencesKey("installation_id")
    }
    private val settings = dataStore.data.map {
        CallUploadSettings(
            it[Keys.enabled] ?: false,
            it[Keys.interval] ?: config.defaultIntervalMinutes,
            it[Keys.wifi] ?: config.defaultWifiOnly,
        )
    }

    override suspend fun enable(intervalMinutes: Long, wifiOnly: Boolean): CallSamplingResult<Unit> {
        if (intervalMinutes !in 20..10_080) {
            return CallSamplingResult.Failure(CallSamplingError.InvalidConfiguration("upload interval must be between 20 minutes and 7 days"))
        }
        val previous = settings.first()
        val next = CallUploadSettings(true, intervalMinutes, wifiOnly)
        return try {
            schedule(next)
            set(next)
            CallSamplingResult.Success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            runCatching { set(previous) }
            CallSamplingResult.Failure(CallSamplingError.SchedulingFailure(error))
        }
    }

    override suspend fun disable(): CallSamplingResult<Unit> = scheduling {
        cancel()
        set(settings.first().copy(enabled = false))
    }

    override suspend fun uploadPending(limit: Int?): CallSamplingResult<Int> {
        if (!settings.first().enabled) return CallSamplingResult.Failure(CallSamplingError.Disabled)
        val runtime = CallUploadRuntime.get() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return CallUploadRuntime.mutex.withLock {
            try {
                val candidates = runtime.calls.uploadQueue.getCandidates((limit ?: config.defaultBatchSize).coerceIn(1, 400))
                if (candidates.isEmpty()) return@withLock CallSamplingResult.Success(0)
                val installId = config.installationIdProvider?.getInstallationId() ?: installationId()
                val items = candidates.map {
                    CallUploadItem(it.session, it.samples, installId, "${Build.MANUFACTURER} ${Build.MODEL}".trim())
                }
                when (val result = config.uploader.upload(items)) {
                    is CallUploaderResult.Failure -> CallSamplingResult.Failure(result.error)
                    is CallUploaderResult.Success -> {
                        runtime.calls.uploadQueue.markUploaded(result.result.uploadedSessionIds.toList())
                        runtime.calls.uploadQueue.markFailed(result.result.rejectedSessionIds.toList())
                        CallSamplingResult.Success(result.result.uploadedSessionIds.size)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                CallSamplingResult.Failure(CallSamplingError.PersistenceFailure(error))
            }
        }
    }

    override suspend fun enqueueUploadNow(): CallSamplingResult<Unit> {
        val current = settings.first()
        if (!current.enabled) return CallSamplingResult.Failure(CallSamplingError.Disabled)
        return scheduling {
            workManager.enqueueUniqueWork(
                CallUploadWorkNames.IMMEDIATE,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<CallUploadWorker>().setConstraints(constraints(current.wifiOnly)).build(),
            )
        }
    }

    override suspend fun reschedule(): CallSamplingResult<Unit> = scheduling {
        val current = settings.first()
        if (current.enabled) schedule(current) else cancel()
    }

    override fun observeSettings(): Flow<CallUploadSettings> = settings

    override fun observeStatus(): Flow<CallUploadStatus> = combine(
        settings,
        workManager.getWorkInfosForUniqueWorkFlow(CallUploadWorkNames.PERIODIC),
    ) { value, work ->
        CallUploadStatus(
            value,
            if (!value.enabled) CallUploadWorkState.DISABLED
            else runCatching { work.firstOrNull()?.state?.name?.let(CallUploadWorkState::valueOf) }.getOrNull()
                ?: CallUploadWorkState.UNKNOWN,
        )
    }

    private suspend fun set(value: CallUploadSettings) = dataStore.edit {
        it[Keys.enabled] = value.enabled
        it[Keys.interval] = value.intervalMinutes
        it[Keys.wifi] = value.wifiOnly
    }

    private suspend fun installationId(): String {
        dataStore.data.first()[Keys.installationId]?.takeIf(String::isNotBlank)?.let { return it }
        val generated = UUID.randomUUID().toString()
        dataStore.edit { if (it[Keys.installationId].isNullOrBlank()) it[Keys.installationId] = generated }
        return dataStore.data.first()[Keys.installationId] ?: generated
    }

    private fun schedule(value: CallUploadSettings) {
        val request = PeriodicWorkRequestBuilder<CallUploadWorker>(value.intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints(value.wifiOnly))
            .build()
        workManager.enqueueUniquePeriodicWork(CallUploadWorkNames.PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun cancel() {
        workManager.cancelUniqueWork(CallUploadWorkNames.PERIODIC)
        workManager.cancelUniqueWork(CallUploadWorkNames.IMMEDIATE)
    }

    private fun constraints(wifiOnly: Boolean) = Constraints.Builder()
        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .build()

    private suspend fun scheduling(block: suspend () -> Unit): CallSamplingResult<Unit> = try {
        block()
        CallSamplingResult.Success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        CallSamplingResult.Failure(CallSamplingError.SchedulingFailure(error))
    }
}

internal class CallUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val runtime = CallUploadRuntime.get() ?: return Result.failure(workDataOf("code" to "NOT_INSTALLED"))
        return when (val result = CallUploadClientImpl(applicationContext, runtime.config).uploadPending()) {
            is CallSamplingResult.Success -> Result.success()
            is CallSamplingResult.Failure -> if (result.error is CallSamplingError.TransientFailure) Result.retry() else Result.failure()
        }
    }
}
