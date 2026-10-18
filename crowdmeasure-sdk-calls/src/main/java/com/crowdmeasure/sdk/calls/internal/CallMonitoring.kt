package com.crowdmeasure.sdk.calls.internal

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.database.SQLException
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.crowdmeasure.sdk.calls.*
import com.crowdmeasure.sdk.model.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "CrowdMeasureCalls"

/** Last-resort handler so a failure in call sampling never crashes the host process. */
internal val callsExceptionHandler = CoroutineExceptionHandler { _, error ->
    Log.e(TAG, "Unhandled call sampling error", error)
}

internal class CallPhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> wasRinging = true
            TelephonyManager.EXTRA_STATE_IDLE -> {
                wasRinging = false
                cellularStartGeneration++
                val pending = goAsync()
                val stop = CallSamplingService.stop(CallSource.CELLULAR)
                if (stop == null) pending.finish() else stop.invokeOnCompletion { pending.finish() }
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val type = if (wasRinging) CallType.INCOMING else CallType.OUTGOING
                wasRinging = false
                val generation = ++cellularStartGeneration
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default + callsExceptionHandler).launch {
                    try {
                        delay(2_000.milliseconds)
                        val rt = CallsRuntime.get() ?: return@launch
                        if (generation != cellularStartGeneration || !context.isCallSourceActive(CallSource.CELLULAR)) {
                            rt.settingsStore.recordMissed(CallRunCode.CALL_NOT_ACTIVE)
                            return@launch
                        }
                        val settings = rt.settingsStore.settings.first()
                        val requirements = rt.requirements()
                        val voipActive = context.isCallSourceActive(CallSource.VOIP_GENERIC)
                        when {
                            voipActive && !settings.voipEnabled -> rt.settingsStore.recordMissed(CallRunCode.DISABLED)
                            !voipActive && !settings.cellularEnabled -> rt.settingsStore.recordMissed(CallRunCode.DISABLED)
                            !requirements.canStart -> rt.settingsStore.recordMissed(requirements.failureCode())
                            voipActive -> CallSamplingService.start(context, CallType.UNKNOWN, CallSource.VOIP_GENERIC)
                            else -> CallSamplingService.start(context, type, CallSource.CELLULAR)
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.e(TAG, "Failed to handle call start", error)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private companion object {
        var wasRinging = false
        var cellularStartGeneration = 0L
    }
}

internal class VoipCallMonitor(
    private val context: Context,
    private val settings: CallsSettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + callsExceptionHandler)
    private var job: Job? = null
    private var detected = false
    private var startGeneration = 0L

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            bestEffort { settings.setVoipActive(true) }
            while (isActive) {
                try {
                    poll()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.e(TAG, "VoIP call monitoring iteration failed", error)
                }
                delay(1_000.milliseconds)
            }
        }
    }

    private suspend fun poll() {
        val active = context.isCallSourceActive(CallSource.VOIP_GENERIC)
        if (active && !detected) {
            val generation = ++startGeneration
            delay(2_000.milliseconds)
            if (generation == startGeneration && context.isCallSourceActive(CallSource.VOIP_GENERIC)) {
                detected = true
                requestStart()
            }
        }
        if (!active) {
            startGeneration++
            if (detected) {
                delay(1_000.milliseconds)
                if (!context.isCallSourceActive(CallSource.VOIP_GENERIC)) {
                    detected = false
                    CallSamplingService.stop(CallSource.VOIP_GENERIC)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        startGeneration++
        if (detected) CallSamplingService.stop(CallSource.VOIP_GENERIC)
        detected = false
        scope.launch { settings.setVoipActive(false) }
    }

    private suspend fun requestStart() {
        if (!context.isCallSourceActive(CallSource.VOIP_GENERIC)) {
            settings.recordMissed(CallRunCode.CALL_NOT_ACTIVE)
            return
        }
        val requirements = CallsRuntime.get()?.requirements() ?: return
        if (!requirements.canStart) {
            settings.recordMissed(requirements.failureCode())
        } else {
            CallSamplingService.start(context, CallType.UNKNOWN, CallSource.VOIP_GENERIC)
        }
    }
}

/**
 * The single owner of call sampling state. [CallSamplingService] only keeps the process in the
 * foreground while a session is active; when the service cannot be started, the sampler runs
 * in-process on its own.
 */
internal class CallSampler(
    private val context: Context,
    private val sdk: com.crowdmeasure.sdk.CrowdMeasureSdk,
    private val config: CallSamplingConfig,
    private val store: CallStore,
    private val settings: CallsSettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + callsExceptionHandler)
    private var sampling: Job? = null
    private val active = MutableStateFlow<CallSession?>(null)
    private var mnoMonitor: DefaultDataMnoMonitor? = null
    private val mnoFinishMutex = Mutex()
    private val lifecycleMutex = Mutex()

    fun launchStart(type: CallType, source: CallSource): Job = scope.launch { start(type, source) }

    fun launchStop(source: CallSource): Job = scope.launch { stop(source) }

    /** Suspends until no session is active. */
    suspend fun awaitIdle() {
        active.first { it == null }
    }

    suspend fun start(type: CallType, source: CallSource) = lifecycleMutex.withLock {
        try {
            startLocked(type, source)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            fail(error)
        }
    }

    private suspend fun startLocked(type: CallType, source: CallSource) {
        active.value?.let { session ->
            if (source == CallSource.VOIP_GENERIC && session.callSource == CallSource.CELLULAR) {
                store.reclassifySession(session.sessionId, CallType.UNKNOWN, CallSource.VOIP_GENERIC)
                active.value = session.copy(callType = CallType.UNKNOWN, callSource = CallSource.VOIP_GENERIC)
            }
            return
        }
        if (!context.isCallSourceActive(source)) {
            settings.recordMissed(CallRunCode.CALL_NOT_ACTIVE)
            return
        }
        val requirements = context.requirements(sdk.requirements.evaluateDefaultDataMno())
        if (!requirements.canStart) {
            settings.recordMissed(requirements.failureCode())
            return
        }
        store.finishActiveSession(System.currentTimeMillis(), "service_restarted")
        store.deleteOlderThan(System.currentTimeMillis() - config.retentionDays * 86_400_000L)
        active.value = store.startSession(type, source, config.sampleIntervalSeconds, context.transportFor(source))
        settings.clearMissed()
        if (requirements.defaultDataMnoEligibility.state !=
            com.crowdmeasure.sdk.DefaultDataMnoEligibilityState.UNRESTRICTED
        ) {
            mnoMonitor = DefaultDataMnoMonitor(context) {
                scope.launch { finishForIneligibleMno() }
            }.also { it.start() }
        }
        sampling = scope.launch {
            try {
                sampleLoop()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                fail(error)
            }
        }
    }

    private suspend fun CoroutineScope.sampleLoop() {
        while (isActive) {
            val session = active.value ?: break
            if (!context.isCallSourceActive(session.callSource)) {
                finishCurrent()
                break
            }
            val eligibility = sdk.requirements.evaluateDefaultDataMno()
            if (!eligibility.allowsCollection) {
                finishForIneligibleMno(eligibility)
                break
            }
            val at = System.currentTimeMillis()
            try {
                val sample = coroutineScope {
                    val cell = async { sdk.cellular.collect() }
                    val location = async {
                        try {
                            sdk.location.collect()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (_: Exception) {
                            null
                        }
                    }
                    val dataUsage = async { sdk.dataUsage.collect() }
                    CallSampleData(
                        cell = cell.await(),
                        location = location.await(),
                        dataUsage = dataUsage.await(),
                    )
                }
                val saved = mnoFinishMutex.withLock {
                    val canSave = active.value?.sessionId == session.sessionId &&
                            sdk.requirements.evaluateDefaultDataMno().allowsCollection
                    if (canSave) {
                        store.insertSample(
                            session.sessionId,
                            at,
                            at - session.startedAtUtcMs,
                            sample.cell,
                            sample.location,
                            sample.dataUsage,
                            context.transportFor(session.callSource),
                        )
                    }
                    canSave
                }
                if (!saved) {
                    finishForIneligibleMno()
                    break
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                recordMissedSafely(CallRunCode.PERSISTENCE_FAILED)
            }
            delay((config.sampleIntervalSeconds * 1_000L).milliseconds)
        }
    }

    private data class CallSampleData(
        val cell: com.crowdmeasure.sdk.model.CellInfo,
        val location: com.crowdmeasure.sdk.model.Location?,
        val dataUsage: com.crowdmeasure.sdk.model.DataUsageInfo?,
    )

    suspend fun stop(source: CallSource) = lifecycleMutex.withLock {
        val session = active.value
        if (session != null && source != CallSource.UNKNOWN && source != session.callSource) {
            return@withLock
        }
        sampling?.cancelAndJoin()
        sampling = null
        mnoMonitor?.stop()
        mnoMonitor = null
        if (session == null) return@withLock
        try {
            store.finishSession(session.sessionId, System.currentTimeMillis(), "call_ended")
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Failed to finish call session", error)
            recordMissedSafely(error.samplingFailureCode())
        } finally {
            active.value = null
        }
    }

    private suspend fun finishCurrent() {
        val session = active.value ?: return
        sampling = null
        mnoMonitor?.stop()
        mnoMonitor = null
        try {
            store.finishSession(session.sessionId, System.currentTimeMillis(), "call_ended")
        } finally {
            active.value = null
        }
    }

    private suspend fun finishForIneligibleMno(
        eligibility: com.crowdmeasure.sdk.DefaultDataMnoEligibility =
            sdk.requirements.evaluateDefaultDataMno(),
    ) {
        if (eligibility.allowsCollection) return
        val session = mnoFinishMutex.withLock {
            active.value?.also {
                active.value = null
                mnoMonitor?.stop()
                mnoMonitor = null
            }
        } ?: return
        store.finishSession(
            session.sessionId,
            System.currentTimeMillis(),
            if (eligibility.state == com.crowdmeasure.sdk.DefaultDataMnoEligibilityState.ROAMING) {
                "roaming"
            } else {
                "target_mno_not_eligible"
            },
        )
        settings.recordMissed(context.requirements(eligibility).failureCode())
    }

    /**
     * Resets in-memory state after an unexpected failure so the next call can start a new
     * session. Must not cancel [sampling]: it may be running inside that job.
     */
    private suspend fun fail(error: Exception) {
        Log.e(TAG, "Call sampling failed", error)
        val session = active.value
        sampling = null
        mnoMonitor?.stop()
        mnoMonitor = null
        active.value = null
        if (session != null) {
            bestEffort { store.finishSession(session.sessionId, System.currentTimeMillis(), "sampling_failed") }
        }
        recordMissedSafely(error.samplingFailureCode())
    }

    private suspend fun recordMissedSafely(code: CallRunCode) {
        bestEffort { settings.recordMissed(code) }
    }
}

/** Runs [block], rethrowing cancellation and logging (then ignoring) any other failure. */
private inline fun <T> bestEffort(block: () -> T): T? = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Log.w(TAG, "Ignoring call sampling failure", error)
    null
}

private fun Throwable.samplingFailureCode(): CallRunCode = when (this) {
    is SQLException, is IOException -> CallRunCode.PERSISTENCE_FAILED
    else -> CallRunCode.UNEXPECTED_ERROR
}

private fun Int?.isVoipMode() =
    this == AudioManager.MODE_IN_COMMUNICATION ||
        (Build.VERSION.SDK_INT >= 33 && this == AudioManager.MODE_COMMUNICATION_REDIRECT)

private fun Context.transportFor(source: CallSource): TransportType {
    if (source == CallSource.CELLULAR) return TransportType.CELL
    val cm = getSystemService(ConnectivityManager::class.java) ?: return TransportType.NONE
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return TransportType.NONE
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TransportType.WIFI
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> TransportType.CELL
        else -> TransportType.OTHER
    }
}

/**
 * Keeps the process in the foreground while [CallSampler] has an active session. Every start
 * command enters the foreground immediately (satisfying the startForegroundService contract),
 * hands the call to the sampler, and stops the service once the sampler is idle again.
 * Stopping is driven by the sampler rather than by a stop intent, because startService is not
 * allowed from the background.
 */
internal class CallSamplingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + callsExceptionHandler)

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rt = CallsRuntime.get()
        if (rt == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val type = intent?.type() ?: CallType.UNKNOWN
        val source = intent?.source() ?: CallSource.UNKNOWN
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification(rt.config),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (error: RuntimeException) {
            Log.w(TAG, "Foreground start failed (${error.foregroundFailureCode()}); sampling in-process", error)
            rt.sampler.launchStart(type, source)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        scope.launch {
            try {
                rt.sampler.start(type, source)
                rt.sampler.awaitIdle()
            } finally {
                // Only stops when this is the most recent start command.
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val rt = CallsRuntime.get() ?: return
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                rt.config.notificationChannelName,
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun notification(config: CallSamplingConfig) =
        NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(config.notificationIconResId)
            .setContentTitle(config.notificationTitle)
            .setContentText(config.notificationText)
            .setOngoing(true)
            .setSilent(true)
            .build()

    companion object {
        private const val CHANNEL = "com.crowdmeasure.sdk.calls.sampling"
        private const val NOTIFICATION_ID = 40_030
        private const val ACTION_START = "com.crowdmeasure.sdk.calls.START"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_SOURCE = "source"

        /**
         * Starts sampling in the foreground service. When Android refuses a foreground start
         * (e.g. background start restrictions on Android 12+), sampling still runs in-process.
         */
        fun start(context: Context, type: CallType, source: CallSource) {
            val rt = CallsRuntime.get() ?: return
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CallSamplingService::class.java).setAction(ACTION_START)
                        .putExtra(EXTRA_TYPE, type.name)
                        .putExtra(EXTRA_SOURCE, source.name)
                )
            } catch (error: RuntimeException) {
                Log.w(TAG, "Foreground service unavailable (${error.foregroundFailureCode()}); sampling in-process", error)
                rt.sampler.launchStart(type, source)
            }
        }

        /** Ends the session for [source]; the service stops itself once the sampler is idle. */
        fun stop(source: CallSource): Job? = CallsRuntime.get()?.sampler?.launchStop(source)
    }
}

@Suppress("DEPRECATION")
private fun Context.isCallSourceActive(source: CallSource): Boolean = when (source) {
    CallSource.CELLULAR -> runCatching {
        getSystemService(TelephonyManager::class.java)?.callState?.let { it != TelephonyManager.CALL_STATE_IDLE } ?: false
    }.getOrDefault(false)
    CallSource.VOIP_GENERIC -> runCatching {
        getSystemService(AudioManager::class.java)?.mode.isVoipMode()
    }.getOrDefault(false)
    else -> true
}

private fun Throwable.foregroundFailureCode(): CallRunCode = when {
    this is SecurityException -> CallRunCode.FOREGROUND_SERVICE_PERMISSION_DENIED
    Build.VERSION.SDK_INT >= 31 && javaClass.name == "android.app.ForegroundServiceStartNotAllowedException" ->
        CallRunCode.FOREGROUND_SERVICE_START_NOT_ALLOWED
    else -> CallRunCode.FOREGROUND_SERVICE_FAILED
}

private fun Intent.type() =
    getStringExtra("type")?.let { runCatching { CallType.valueOf(it) }.getOrNull() }
        ?: CallType.UNKNOWN

private fun Intent.source() =
    getStringExtra("source")?.let { runCatching { CallSource.valueOf(it) }.getOrNull() }
        ?: CallSource.UNKNOWN
