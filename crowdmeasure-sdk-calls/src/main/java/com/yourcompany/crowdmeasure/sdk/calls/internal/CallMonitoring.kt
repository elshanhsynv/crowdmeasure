package com.crowdmeasure.sdk.calls.internal

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.*
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.crowdmeasure.sdk.calls.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

internal class CallPhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_RINGING -> wasRinging = true
            TelephonyManager.EXTRA_STATE_IDLE -> {
                wasRinging = false; CallSamplingService.stop(context, CallSource.CELLULAR)
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val type = if (wasRinging) CallType.INCOMING else CallType.OUTGOING
                wasRinging = false
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        delay(2_000)
                        val rt = CallsRuntime.get() ?: return@launch
                        val settings = rt.settingsStore.settings.first()
                        val requirements = context.requirements()
                        if (!settings.cellularEnabled) rt.settingsStore.recordMissed(CallRunCode.DISABLED)
                        else if (!requirements.canStart) rt.settingsStore.recordMissed(requirements.failureCode())
                        else runCatching {
                            CallSamplingService.start(
                                context,
                                type,
                                CallSource.CELLULAR
                            )
                        }
                            .onFailure { rt.settingsStore.recordMissed(CallRunCode.FOREGROUND_SERVICE_FAILED) }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private companion object {
        var wasRinging = false
    }
}

internal class VoipCallMonitor(
    private val context: Context,
    private val settings: CallsSettingsStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var detected = false
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            settings.setVoipActive(true)
            while (isActive) {
                val active = context.getSystemService(AudioManager::class.java)?.mode.isVoipMode()
                if (active && !detected) {
                    delay(2_000); if (context.getSystemService(AudioManager::class.java)?.mode.isVoipMode()) {
                        detected = true; requestStart()
                    }
                }
                if (!active && detected) {
                    delay(3_000); if (!context.getSystemService(AudioManager::class.java)?.mode.isVoipMode()) {
                        detected = false; CallSamplingService.stop(context, CallSource.VOIP_GENERIC)
                    }
                }
                delay(1_000)
            }
        }
    }

    fun stop() {
        job?.cancel(); job = null; if (detected) CallSamplingService.stop(
            context,
            CallSource.VOIP_GENERIC
        ); detected = false; scope.launch { settings.setVoipActive(false) }
    }

    private suspend fun requestStart() {
        val requirements = context.requirements()
        if (!requirements.canStart) settings.recordMissed(requirements.failureCode()) else runCatching {
            CallSamplingService.start(
                context,
                CallType.UNKNOWN,
                CallSource.VOIP_GENERIC
            )
        }.onFailure { settings.recordMissed(CallRunCode.FOREGROUND_SERVICE_FAILED) }
    }
}

private fun Int?.isVoipMode() =
    this == AudioManager.MODE_IN_COMMUNICATION || (Build.VERSION.SDK_INT >= 33 && this == AudioManager.MODE_COMMUNICATION_REDIRECT)

internal class CallSamplingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var sampling: Job? = null
    private var active: CallSession? = null
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate(); createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val rt = CallsRuntime.get() ?: run { stopSelf(); return START_NOT_STICKY }
        scope.launch {
            when (intent?.action) {
                ACTION_START -> startSampling(rt, intent.type(), intent.source())
                ACTION_STOP -> stopSampling(rt, intent.source())
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel(); super.onDestroy()
    }

    private suspend fun startSampling(
        rt: InstalledCallsRuntime,
        type: CallType,
        source: CallSource
    ) {
        active?.let { session ->
            if (source == CallSource.VOIP_GENERIC && session.callSource == CallSource.CELLULAR) {
                rt.store.reclassifySession(
                    session.sessionId,
                    CallType.UNKNOWN,
                    CallSource.VOIP_GENERIC
                )
                active =
                    session.copy(callType = CallType.UNKNOWN, callSource = CallSource.VOIP_GENERIC)
            }
            return
        }
        val requirements = applicationContext.requirements()
        if (!requirements.canStart) {
            rt.settingsStore.recordMissed(requirements.failureCode()); stopSelf(); return
        }
        val config = rt.config
        try {
            ServiceCompat.startForeground(
                this,
                40_030,
                notification(config),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (error: RuntimeException) {
            rt.settingsStore.recordMissed(CallRunCode.FOREGROUND_SERVICE_FAILED)
            stopSelf()
            return
        }
        rt.store.finishActiveSession(System.currentTimeMillis(), "service_restarted")
        rt.store.deleteOlderThan(System.currentTimeMillis() - config.retentionDays * 86_400_000L)
        active = rt.store.startSession(type, source, config.sampleIntervalSeconds)
        sampling = scope.launch {
            while (isActive) {
                val session = active ?: break
                val at = System.currentTimeMillis()
                try {
                    rt.store.insertSample(
                        session.sessionId,
                        at,
                        at - session.startedAtUtcMs,
                        rt.sdk.cellular.collect()
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    rt.settingsStore.recordMissed(CallRunCode.PERSISTENCE_FAILED)
                }
                delay(config.sampleIntervalSeconds * 1_000L)
            }
        }
    }

    private suspend fun stopSampling(rt: InstalledCallsRuntime, source: CallSource) {
        val session = active ?: return
        if (source != CallSource.UNKNOWN && source != session.callSource) return
        sampling?.cancelAndJoin(); sampling = null
        rt.store.finishSession(session.sessionId, System.currentTimeMillis(), "call_ended")
        active = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE); stopSelf()
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
        NotificationCompat.Builder(this, CHANNEL).setSmallIcon(config.notificationIconResId)
            .setContentTitle(config.notificationTitle).setContentText(config.notificationText)
            .setOngoing(true).setSilent(true).build()

    companion object {
        private const val CHANNEL = "com.crowdmeasure.sdk.calls.sampling"
        private const val ACTION_START = "com.crowdmeasure.sdk.calls.START"
        private const val ACTION_STOP = "com.crowdmeasure.sdk.calls.STOP"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_SOURCE = "source"
        fun start(context: Context, type: CallType, source: CallSource) =
            ContextCompat.startForegroundService(
                context,
                Intent(context, CallSamplingService::class.java).setAction(ACTION_START)
                    .putExtra(EXTRA_TYPE, type.name).putExtra(EXTRA_SOURCE, source.name)
            )

        fun stop(context: Context, source: CallSource) {
            context.startService(
                Intent(context, CallSamplingService::class.java).setAction(
                    ACTION_STOP
                ).putExtra(EXTRA_SOURCE, source.name)
            )
        }
    }
}

private fun Intent.type() =
    getStringExtra("type")?.let { runCatching { CallType.valueOf(it) }.getOrNull() }
        ?: CallType.UNKNOWN

private fun Intent.source() =
    getStringExtra("source")?.let { runCatching { CallSource.valueOf(it) }.getOrNull() }
        ?: CallSource.UNKNOWN
