package com.crowdmeasure.sdk.calls.internal

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.crowdmeasure.sdk.calls.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

internal class CallSamplingClientImpl(private val context: Context) : CallSamplingClient {
    private fun runtime() = CallsRuntime.get()

    override val uploadQueue: CallUploadQueueClient = object : CallUploadQueueClient {
        override suspend fun getCandidates(limit: Int): List<CallSessionExport> =
            runtime()?.store?.getUploadCandidates(limit.coerceIn(1, 400)).orEmpty()

        override suspend fun markUploaded(sessionIds: List<String>): CallSamplingResult<Unit> =
            persist { markUploaded(sessionIds.distinct()) }

        override suspend fun markFailed(sessionIds: List<String>): CallSamplingResult<Unit> =
            persist { markFailed(sessionIds.distinct()) }

        private suspend fun persist(action: suspend CallStore.() -> Unit): CallSamplingResult<Unit> {
            val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
            return operational { rt.store.action() }
        }
    }

    override suspend fun setCellularSamplingEnabled(enabled: Boolean): CallSamplingResult<Unit> {
        val result = setting { it.copy(cellularEnabled = enabled) }
        if (!enabled && result is CallSamplingResult.Success) {
            runCatching { CallSamplingService.stop(context, CallSource.CELLULAR) }
        }
        return result
    }

    override suspend fun setVoipSamplingEnabled(enabled: Boolean): CallSamplingResult<Unit> {
        val result = setting { it.copy(voipEnabled = enabled) }
        if (result is CallSamplingResult.Success) {
            if (enabled) runtime()?.monitor?.start() else runtime()?.monitor?.stop()
        }
        return result
    }

    override suspend fun activateEnabledFeatures(): CallSamplingResult<Unit> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return operational {
            val settings = rt.settingsStore.settings.first()
            if (settings.voipEnabled) rt.monitor.start() else rt.monitor.stop()
        }
    }

    override fun observeSettings(): Flow<CallSamplingSettings> =
        runtime()?.settingsStore?.settings ?: flowOf(CallSamplingSettings())

    override fun observeRequirements(): Flow<CallSamplingRequirements> = flow {
        while (true) {
            emit(context.requirements())
            delay(2_000.milliseconds)
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
            )
        )
        return combine(
            rt.settingsStore.settings,
            rt.settingsStore.missed,
            rt.settingsStore.voipActive,
            rt.store.observeSessions(1),
        ) { settings, missed, voip, sessions ->
            CallSamplingStatus(
                settings,
                context.requirements(),
                sessions.firstOrNull { it.endedAtUtcMs == null },
                voip,
                missed,
            )
        }
    }

    override fun observeSessions(limit: Int) =
        runtime()?.store?.observeSessions(limit.coerceIn(1, 1_000)) ?: flowOf(emptyList())

    override fun observeSamples(sessionId: String) =
        runtime()?.store?.observeSamples(sessionId) ?: flowOf(emptyList())

    private val jsonP: Json by lazy {
        Json { prettyPrint = true }
    }

    override suspend fun exportSessions(lastN: Int): CallSamplingResult<Uri> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return try {
            val sessions = rt.store.getRecentSessions(lastN.coerceIn(1, 10_000))
            val dir = File(context.cacheDir, "crowdmeasure-call-exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "crowdmeasure_call_sessions_$stamp.json")
            file.writeText(jsonP.encodeToString(JsonObject.serializer(), callExportJson(sessions)))
            CallSamplingResult.Success(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.crowdmeasure-calls.fileprovider",
                    file
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            CallSamplingResult.Failure(CallSamplingError.ExportFailure(error))
        }
    }

    override suspend fun deleteAll(): CallSamplingResult<Unit> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return operational { rt.store.deleteAll() }
    }

    private suspend fun setting(transform: (CallSamplingSettings) -> CallSamplingSettings): CallSamplingResult<Unit> {
        val rt = runtime() ?: return CallSamplingResult.Failure(CallSamplingError.NotInstalled)
        return operational { rt.settingsStore.set(transform(rt.settingsStore.settings.first())) }
    }

    private suspend fun operational(block: suspend () -> Unit): CallSamplingResult<Unit> = try {
        block()
        CallSamplingResult.Success(Unit)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        CallSamplingResult.Failure(CallSamplingError.PersistenceFailure(error))
    }
}

private fun callExportJson(items: List<CallSessionExport>) = buildJsonObject {
    put("schema_version", 1)
    put("exported_at_utc_ms", System.currentTimeMillis())
    put("session_count", items.size)
    putJsonArray("sessions") {
        items.forEach { item ->
            add(Json.encodeToJsonElement(item.session).jsonObject.toMutableMap().let { session ->
                session["samples"] = Json.encodeToJsonElement(item.samples)
                JsonObject(session)
            })
        }
    }
}
