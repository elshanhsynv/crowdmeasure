package com.crowdmeasure.sdk.calls.internal

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.crowdmeasure.sdk.calls.*
import com.crowdmeasure.sdk.model.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

internal class CallPhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> wasRinging = true
            TelephonyManager.EXTRA_STATE_IDLE -> {
                wasRinging = false
                cellularStartGeneration++
                CallSamplingService.stop(context, CallSource.CELLULAR)
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val type = if (wasRinging) CallType.INCOMING else CallType.OUTGOING
                wasRinging = false
                val generation = ++cellularStartGeneration
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
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
                            else -> runCatching {
                                if (voipActive) {
                                    CallSamplingService.start(context, CallType.UNKNOWN, CallSource.VOIP_GENERIC)
                                } else {
                                    CallSamplingService.start(context, type, CallSource.CELLULAR)
                                }
                            }.onFailure { rt.settingsStore.recordMissed(it.foregroundFailureCode()) }
                        }
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var detected = false
    private var startGeneration = 0L

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            settings.setVoipActive(true)
            while (isActive) {
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
                            CallSamplingService.stop(context, CallSource.VOIP_GENERIC)
                        }
                    }
                }
                delay(1_000.milliseconds)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        startGeneration++
        if (detected) CallSamplingService.stop(context, CallSource.VOIP_GENERIC)
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
            runCatching {
                CallSamplingService.start(context, CallType.UNKNOWN, CallSource.VOIP_GENERIC)
            }.onFailure { settings.recordMissed(it.foregroundFailureCode()) }
        }
    }
}

internal class CallSampler(
    private val context: Context,
    private val sdk: com.crowdmeasure.sdk.CrowdMeasureSdk,
    private val config: CallSamplingConfig,
    private val store: CallStore,
    private val settings: CallsSettingsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sampling: Job? = null
    private var active: CallSession? = null
    private var mnoMonitor: DefaultDataMnoMonitor? = null
    private val mnoFinishMutex = Mutex()

    suspend fun start(type: CallType, source: CallSource) {
        active?.let { session ->
            if (source == CallSource.VOIP_GENERIC && session.callSource == CallSource.CELLULAR) {
                store.reclassifySession(session.sessionId, CallType.UNKNOWN, CallSource.VOIP_GENERIC)
                active = session.copy(callType = CallType.UNKNOWN, callSource = CallSource.VOIP_GENERIC)
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
        active = store.startSession(type, source, config.sampleIntervalSeconds, context.transportFor(source))
        settings.clearMissed()
        if (requirements.defaultDataMnoEligibility.state !=
            com.crowdmeasure.sdk.DefaultDataMnoEligibilityState.UNRESTRICTED
        ) {
            mnoMonitor = DefaultDataMnoMonitor(context) {
                scope.launch { finishForIneligibleMno() }
            }.also { it.start() }
        }
        sampling = scope.launch {
            while (isActive) {
                val session = active ?: break
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
                        val canSave = active?.sessionId == session.sessionId &&
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
                    settings.recordMissed(CallRunCode.PERSISTENCE_FAILED)
                }
                delay((config.sampleIntervalSeconds * 1_000L).milliseconds)
            }
        }
    }

    private data class CallSampleData(
        val cell: com.crowdmeasure.sdk.model.CellInfo,
        val location: com.crowdmeasure.sdk.model.Location?,
        val dataUsage: com.crowdmeasure.sdk.model.DataUsageInfo?,
    )

    suspend fun stop(source: CallSource) {
        val session = active ?: run {
            sampling?.cancelAndJoin()
            sampling = null
            return
        }
        if (source != CallSource.UNKNOWN && source != session.callSource) return
        sampling?.cancelAndJoin()
        sampling = null
        mnoMonitor?.stop()
        mnoMonitor = null
        store.finishSession(session.sessionId, System.currentTimeMillis(), "call_ended")
        active = null
    }

    private suspend fun finishCurrent() {
        val session = active ?: return
        sampling = null
        mnoMonitor?.stop()
        mnoMonitor = null
        store.finishSession(session.sessionId, System.currentTimeMillis(), "call_ended")
        active = null
    }

    private suspend fun finishForIneligibleMno(
        eligibility: com.crowdmeasure.sdk.DefaultDataMnoEligibility =
            sdk.requirements.evaluateDefaultDataMno(),
    ) {
        if (eligibility.allowsCollection) return
        val session = mnoFinishMutex.withLock {
            active?.also {
                active = null
                mnoMonitor?.stop()
                mnoMonitor = null
            }
        } ?: return
        store.finishSession(session.sessionId, System.currentTimeMillis(), "target_mno_not_eligible")
        settings.recordMissed(context.requirements(eligibility).failureCode())
    }
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

internal class CallSamplingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sampling: Job? = null
    private var active: CallSession? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rt = CallsRuntime.get() ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch {
            when (intent?.action) {
                ACTION_START -> startSampling(rt, intent.type(), intent.source())
                ACTION_STOP -> stopSampling(rt, intent.source())
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun startSampling(
        rt: InstalledCallsRuntime,
        type: CallType,
        source: CallSource
    ) {
        active?.let { session ->
            if (source == CallSource.VOIP_GENERIC && session.callSource == CallSource.CELLULAR) {
                rt.store.reclassifySession(session.sessionId, CallType.UNKNOWN, CallSource.VOIP_GENERIC)
                active = session.copy(callType = CallType.UNKNOWN, callSource = CallSource.VOIP_GENERIC)
            }
            return
        }
        if (!applicationContext.isCallSourceActive(source)) {
            rt.settingsStore.recordMissed(CallRunCode.CALL_NOT_ACTIVE)
            stopSelf()
            return
        }
        val requirements = rt.requirements()
        if (!requirements.canStart) {
            rt.settingsStore.recordMissed(requirements.failureCode())
            stopSelf()
            return
        }
        val config = rt.config
        try {
            ServiceCompat.startForeground(
                this,
                40_030,
                notification(config),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (error: SecurityException) {
            rt.settingsStore.recordMissed(CallRunCode.FOREGROUND_SERVICE_PERMISSION_DENIED)
            stopSelf()
            return
        } catch (error: RuntimeException) {
            rt.settingsStore.recordMissed(error.foregroundFailureCode())
            stopSelf()
            return
        }
        if (!applicationContext.isCallSourceActive(source)) {
            rt.settingsStore.recordMissed(CallRunCode.CALL_NOT_ACTIVE)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        rt.store.finishActiveSession(System.currentTimeMillis(), "service_restarted")
        rt.store.deleteOlderThan(System.currentTimeMillis() - config.retentionDays * 86_400_000L)
        active = rt.store.startSession(type, source, config.sampleIntervalSeconds, applicationContext.transportFor(source))
        rt.settingsStore.clearMissed()
        sampling = scope.launch {
            while (isActive) {
                val session = active ?: break
                if (!applicationContext.isCallSourceActive(session.callSource)) {
                    finishCurrentSession(rt)
                    break
                }
                val eligibility = rt.sdk.requirements.evaluateDefaultDataMno()
                if (!eligibility.allowsCollection) {
                    rt.store.finishSession(
                        session.sessionId,
                        System.currentTimeMillis(),
                        "target_mno_not_eligible",
                    )
                    active = null
                    rt.settingsStore.recordMissed(applicationContext.requirements(eligibility).failureCode())
                    ServiceCompat.stopForeground(this@CallSamplingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                val at = System.currentTimeMillis()
                try {
                    coroutineScope {
                        val cell = async { rt.sdk.cellular.collect() }
                        val location = async {
                            try {
                                rt.sdk.location.collect()
                            } catch (error: CancellationException) {
                                throw error
                            } catch (_: Exception) {
                                null
                            }
                        }
                        val dataUsage = async { rt.sdk.dataUsage.collect() }
                        val collectedCell = cell.await()
                        val collectedLocation = location.await()
                        val collectedDataUsage = dataUsage.await()
                        if (!rt.sdk.requirements.evaluateDefaultDataMno().allowsCollection) {
                            return@coroutineScope
                        }
                        rt.store.insertSample(
                            session.sessionId,
                            at,
                            at - session.startedAtUtcMs,
                            collectedCell,
                            collectedLocation,
                            collectedDataUsage,
                            applicationContext.transportFor(session.callSource),
                        )
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    rt.settingsStore.recordMissed(CallRunCode.PERSISTENCE_FAILED)
                }
                delay((config.sampleIntervalSeconds * 1_000L).milliseconds)
            }
        }
    }

    private suspend fun finishCurrentSession(rt: InstalledCallsRuntime) {
        val session = active ?: return
        sampling = null
        rt.store.finishSession(session.sessionId, System.currentTimeMillis(), "call_ended")
        active = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun stopSampling(rt: InstalledCallsRuntime, source: CallSource) {
        val session = active ?: run {
            sampling?.cancelAndJoin()
            sampling = null
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (source != CallSource.UNKNOWN && source != session.callSource) return
        sampling?.cancelAndJoin()
        sampling = null
        rt.store.finishSession(session.sessionId, System.currentTimeMillis(), "call_ended")
        active = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
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
        private const val ACTION_START = "com.crowdmeasure.sdk.calls.START"
        private const val ACTION_STOP = "com.crowdmeasure.sdk.calls.STOP"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_SOURCE = "source"

        fun start(context: Context, type: CallType, source: CallSource) =
            CallsRuntime.get()?.let { rt ->
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    rt.sampler.start(type, source)
                }
            } ?: Unit

        fun stop(context: Context, source: CallSource) {
            CallsRuntime.get()?.let { rt ->
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    rt.sampler.stop(source)
                }
            }
        }
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
