package com.yourcompany.crowdmeasure.sdk.calls.internal

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.work.*
import com.yourcompany.crowdmeasure.sdk.calls.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal object CallWorkNames {
    const val PREFIX = "com.yourcompany.crowdmeasure.sdk.calls"
    const val UPLOAD_PERIODIC = "$PREFIX.upload.periodic"
    const val UPLOAD_IMMEDIATE = "$PREFIX.upload.immediate"
    const val TAG = "$PREFIX.owned"
}

internal class CallSamplingClientImpl(private val context: Context) : CallSamplingClient {
    private val workManager = WorkManager.getInstance(context)
    private fun runtime() = CallsRuntime.get()

    override suspend fun setCellularSamplingEnabled(enabled: Boolean) =
        setting { it.copy(cellularEnabled = enabled) }

    override suspend fun setVoipSamplingEnabled(enabled: Boolean): CallSamplingResult<Unit> {
        val result = setting { it.copy(voipEnabled = enabled) }
        if (result is CallSamplingResult.Success) {
            if (enabled) runtime()?.monitor?.start() else runtime()?.monitor?.stop()
        }
        return result
    }

    override suspend fun activateEnabledFeatures(): CallSamplingResult<Unit> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return runCatching {
            val settings = rt.settingsStore.settings.first()
            if (settings.voipEnabled) rt.monitor.start() else rt.monitor.stop()
            scheduleUploads(settings)
        }.fold(
            { CallSamplingResult.Success(Unit) },
            { CallSamplingResult.Failure(CallSamplingError.SchedulingFailure(it)) })
    }

    override fun observeSettings(): Flow<CallSamplingSettings> =
        runtime()?.settingsStore?.settings ?: flowOf(CallSamplingSettings())

    override fun observeRequirements(): Flow<CallSamplingRequirements> = flow {
        while (true) {
            emit(context.requirements()); kotlinx.coroutines.delay(2_000)
        }
    }.distinctUntilChanged()

    override fun observeStatus(): Flow<CallSamplingStatus> {
        val rt = runtime() ?: return flowOf(
            CallSamplingStatus(
                CallSamplingSettings(),
                context.requirements(),
                null,
                false,
                MissedCallStart(System.currentTimeMillis(), CallRunCode.NOT_INSTALLED),
                CallUploadWorkState.UNKNOWN
            )
        )
        return combine(
            rt.settingsStore.settings,
            rt.settingsStore.missed,
            rt.settingsStore.voipActive,
            rt.store.observeSessions(1),
            workManager.getWorkInfosForUniqueWorkFlow(CallWorkNames.UPLOAD_PERIODIC)
        ) { settings, missed, voip, sessions, work ->
            CallSamplingStatus(
                settings,
                context.requirements(),
                sessions.firstOrNull { it.endedAtUtcMs == null },
                voip,
                missed,
                if (!settings.uploadsEnabled) CallUploadWorkState.DISABLED else work.firstOrNull()?.state.toCallState()
            )
        }
    }

    override fun observeSessions(limit: Int) =
        runtime()?.store?.observeSessions(limit.coerceIn(1, 1_000)) ?: flowOf(emptyList())

    override fun observeSamples(sessionId: String) =
        runtime()?.store?.observeSamples(sessionId) ?: flowOf(emptyList())

    override suspend fun setUploadsEnabled(
        enabled: Boolean,
        intervalMinutes: Long,
        wifiOnly: Boolean
    ): CallSamplingResult<Unit> {
        if (intervalMinutes !in CrowdMeasureCalls.MIN_UPLOAD_INTERVAL_MINUTES..CrowdMeasureCalls.MAX_UPLOAD_INTERVAL_MINUTES) return CallSamplingResult.Failure(
            CallSamplingError.InvalidConfiguration("upload interval must be between 20 minutes and 7 days")
        )
        val result = setting {
            it.copy(
                uploadsEnabled = enabled,
                uploadIntervalMinutes = intervalMinutes,
                uploadWifiOnly = wifiOnly
            )
        }
        if (result is CallSamplingResult.Success) scheduleUploads(runtime()!!.settingsStore.settings.first())
        return result
    }

    override suspend fun uploadPending(limit: Int): CallSamplingResult<Int> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        if (!rt.settingsStore.settings.first().uploadsEnabled) return CallSamplingResult.Failure(
            CallSamplingError.Disabled
        )
        val uploader =
            rt.uploader ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return CallsRuntime.uploadMutex.withLock {
            val candidates = runCatching {
                rt.store.getUploadCandidates(
                    limit.coerceIn(
                        1,
                        400
                    )
                )
            }.getOrElse {
                return@withLock CallSamplingResult.Failure(
                    CallSamplingError.PersistenceFailure(
                        it
                    )
                )
            }
            if (candidates.isEmpty()) return@withLock CallSamplingResult.Success(0)
            val installId = rt.installationIdProvider.getInstallationId()
            val items = candidates.map {
                CallUploadItem(
                    it.session,
                    it.samples,
                    installId,
                    "${Build.MANUFACTURER} ${Build.MODEL}".trim()
                )
            }
            when (val result = uploader.upload(items)) {
                is CallUploaderResult.Success -> runCatching { rt.store.markUploaded(result.result.uploadedSessionIds); result.result.uploadedSessionIds.size }
                    .fold(
                        { CallSamplingResult.Success(it) },
                        { CallSamplingResult.Failure(CallSamplingError.PersistenceFailure(it)) })

                is CallUploaderResult.Failure -> {
                    if (result.error !is CallSamplingError.TransientFailure) rt.store.markFailed(
                        candidates.map { it.session.sessionId })
                    CallSamplingResult.Failure(result.error)
                }
            }
        }
    }

    override suspend fun enqueueUploadNow(): CallSamplingResult<Unit> {
        val settings =
            runtime()?.settingsStore?.settings?.first() ?: return CallSamplingResult.Failure(
                CallSamplingError.NotInstalled
            )
        if (!settings.uploadsEnabled) return CallSamplingResult.Failure(CallSamplingError.Disabled)
        return scheduling {
            val request =
                OneTimeWorkRequestBuilder<CallUploadWorker>().setConstraints(constraints(settings.uploadWifiOnly))
                    .addTag(CallWorkNames.TAG).build()
            workManager.enqueueUniqueWork(
                CallWorkNames.UPLOAD_IMMEDIATE,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun exportSessions(lastN: Int): CallSamplingResult<Uri> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return runCatching {
            val sessions = rt.store.getRecentSessions(lastN.coerceIn(1, 10_000))
            val dir = File(context.cacheDir, "crowdmeasure-call-exports").apply { mkdirs() }
            val file = File(
                dir,
                "crowdmeasure_call_sessions_${
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(
                        Date()
                    )
                }.json"
            )
            file.writeText(
                Json { prettyPrint = true }.encodeToString(
                    JsonObject.serializer(),
                    callExportJson(sessions)
                )
            )
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.crowdmeasure-calls.fileprovider",
                file
            )
        }.fold(
            { CallSamplingResult.Success(it) },
            { CallSamplingResult.Failure(CallSamplingError.ExportFailure(it)) })
    }

    override suspend fun deleteAll(): CallSamplingResult<Unit> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return runCatching { rt.store.deleteAll() }.fold(
            { CallSamplingResult.Success(Unit) },
            { CallSamplingResult.Failure(CallSamplingError.PersistenceFailure(it)) })
    }

    private suspend fun setting(transform: (CallSamplingSettings) -> CallSamplingSettings): CallSamplingResult<Unit> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return runCatching { rt.settingsStore.set(transform(rt.settingsStore.settings.first())) }.fold(
            { CallSamplingResult.Success(Unit) },
            { CallSamplingResult.Failure(CallSamplingError.PersistenceFailure(it)) })
    }

    private fun scheduleUploads(settings: CallSamplingSettings) {
        if (!settings.uploadsEnabled) {
            workManager.cancelUniqueWork(CallWorkNames.UPLOAD_PERIODIC); workManager.cancelUniqueWork(
                CallWorkNames.UPLOAD_IMMEDIATE
            ); return
        }
        val request = PeriodicWorkRequestBuilder<CallUploadWorker>(
            settings.uploadIntervalMinutes,
            TimeUnit.MINUTES
        ).setConstraints(constraints(settings.uploadWifiOnly)).addTag(CallWorkNames.TAG).build()
        workManager.enqueueUniquePeriodicWork(
            CallWorkNames.UPLOAD_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun constraints(wifiOnly: Boolean) = Constraints.Builder()
        .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
        .build()

    private inline fun scheduling(block: () -> Unit) = runCatching(block).fold(
        { CallSamplingResult.Success(Unit) },
        { CallSamplingResult.Failure(CallSamplingError.SchedulingFailure(it)) })
}

private fun WorkInfo.State?.toCallState() =
    runCatching { this?.name?.let(CallUploadWorkState::valueOf) }.getOrNull()
        ?: CallUploadWorkState.UNKNOWN

private fun callExportJson(items: List<CallSessionExport>) = buildJsonObject {
    put("schema_version", 1); put(
    "exported_at_utc_ms",
    System.currentTimeMillis()
); put("session_count", items.size)
    putJsonArray("sessions") {
        items.forEach { item ->
            add(buildJsonObject {
                put("session_id", item.session.sessionId); put(
                "started_at_utc_ms",
                item.session.startedAtUtcMs
            ); item.session.endedAtUtcMs?.let { put("ended_at_utc_ms", it) }
                put("call_type", item.session.callType.name); put(
                "call_source",
                item.session.callSource.name
            ); put(
                "sample_interval_seconds",
                item.session.sampleIntervalSeconds
            ); put("sample_count", item.session.sampleCount)
                item.session.endReason?.let {
                    put(
                        "end_reason",
                        it
                    )
                }; putJsonArray("samples") {
                    item.samples.forEach { sample ->
                        add(buildJsonObject {
                            put("id", sample.id)
                            put("session_id", sample.sessionId)
                            put("sampled_at_utc_ms", sample.sampledAtUtcMs)
                            put("elapsed_ms", sample.elapsedMs)
                            sample.rat?.let { put("rat", it) }
                            sample.nrState?.let { put("nr_state", it) }
                            sample.dbm?.let { put("dbm", it) }
                            sample.rsrpDbm?.let { put("rsrp_dbm", it) }
                            sample.rsrqDb?.let { put("rsrq_db", it) }
                            sample.sinrDb?.let { put("sinr_db", it) }
                            sample.pci?.let { put("pci", it) }
                            sample.tac?.let { put("tac", it) }
                            sample.band?.let { put("band", it) }
                            put("cell", Json.encodeToJsonElement(sample.cell))
                        })
                    }
                }
            })
        }
    }
}
