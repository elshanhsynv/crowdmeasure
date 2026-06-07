package com.example.crowdmeasure.callsampling

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.crowdmeasure.R
import com.example.crowdmeasure.data.measurement.collectors.TelephonyCollector
import com.example.crowdmeasure.data.prefs.CallSamplingStatusStore
import com.example.crowdmeasure.di.IoDispatcher
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.model.CallSource
import com.example.crowdmeasure.domain.model.CallType
import com.example.crowdmeasure.domain.repo.CallSamplingRepository
import com.example.crowdmeasure.workers.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class CallSamplingService : Service() {
    @Inject
    lateinit var repository: CallSamplingRepository

    @Inject
    lateinit var statusStore: CallSamplingStatusStore

    @Inject
    @IoDispatcher
    lateinit var io: CoroutineDispatcher

    @Inject
    lateinit var workScheduler: WorkScheduler
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var samplingJob: Job? = null
    private var activeSession: CallSession? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var foregroundStarted = false
    private var callType: CallType = CallType.UNKNOWN
    private var callSource: CallSource = CallSource.UNKNOWN

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requestedCallType = intent?.getStringExtra(EXTRA_CALL_TYPE)
            ?.let { runCatching { CallType.valueOf(it) }.getOrNull() } ?: CallType.UNKNOWN
        val requestedCallSource = intent?.getStringExtra(EXTRA_CALL_SOURCE)
            ?.let { runCatching { CallSource.valueOf(it) }.getOrNull() } ?: CallSource.UNKNOWN

        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch {
                    stopSampling(
                        endReason = END_REASON_CALL_ENDED, requestedSource = requestedCallSource
                    )
                }
            }

            else -> {
                if (samplingJob?.isActive == true) {
                    Timber.tag("CallSamplingService")
                        .d("Ignoring start while ${callSource.name} sampling is active.")
                    return START_STICKY
                }
                callType = requestedCallType
                callSource = requestedCallSource
                startInForeground()
                serviceScope.launch {
                    Timber.tag("CallSamplingService").d("Started sampling during call.")
                    startSampling()
                    Timber.tag("CallSamplingService")
                        .d("Call source = ${callSource.name}, type = ${callType.name}")
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterCallStateListener()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun startSampling() {
        if (samplingJob?.isActive == true) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            statusStore.recordMissedStart("android_q_required")
            stopSelf()
            return
        }

        repository.deleteOlderThan(System.currentTimeMillis() - RETENTION_MS)
        if (callSource == CallSource.CELLULAR) {
            registerCallStateListener()
        }
        val session = repository.startSession(callType, callSource, SAMPLE_INTERVAL_SECONDS)
        activeSession = session

        samplingJob = serviceScope.launch {
            while (isActive) {
                collectSample(session)
                delay((SAMPLE_INTERVAL_SECONDS * 1_000L).milliseconds)
            }
        }
    }

    private suspend fun stopSampling(
        endReason: String, requestedSource: CallSource = CallSource.UNKNOWN
    ) {
        if (activeSession == null && samplingJob == null) {
            stopSelf()
            return
        }
        if (!requestedSource.matchesActiveSource(callSource)) return

        val session = activeSession
        samplingJob?.cancelAndJoin()
        samplingJob = null
        unregisterCallStateListener()

        if (session != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collectSample(session)
            }
            val now = System.currentTimeMillis()
            repository.finishSession(session.sessionId, now, endReason)
            repository.deleteOlderThan(now - RETENTION_MS)
            workScheduler.kickoffCallUploadOnce()
            activeSession = null
        }

        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun collectSample(session: CallSession) {
        try {
            val sampledAt = System.currentTimeMillis()
            val cell = withContext(io) { TelephonyCollector.collect(applicationContext) }
            repository.insertSample(
                sessionId = session.sessionId,
                sampledAtUtcMs = sampledAt,
                elapsedMs = sampledAt - session.startedAtUtcMs,
                cellInfo = cell,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Keep the call session alive; transient telephony read failures should not end sampling.
        }
    }

    private fun startInForeground() {
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(), foregroundType
        )
        foregroundStarted = true
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.crowdmeasure)
            .setContentTitle("Collecting call cell stats").setContentText(notificationBody())
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_LOW).build()

    private fun notificationBody(): String = if (callSource.isWhatsapp()) {
        "Sampling cellular signal during the WhatsApp call."
    } else {
        "Sampling cellular signal during the active call."
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Call cell sampling", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun registerCallStateListener() {
        val telephonyManager = getSystemService<TelephonyManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = CallStateCallback()
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            telephonyCallback = callback
        } else {
            @Suppress("DEPRECATION") val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated by Android framework")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) {
                        serviceScope.launch {
                            stopSampling(
                                endReason = END_REASON_CALL_ENDED,
                                requestedSource = CallSource.CELLULAR
                            )
                        }
                    }
                }
            }
            @Suppress("DEPRECATION") telephonyManager.listen(
                listener,
                PhoneStateListener.LISTEN_CALL_STATE
            )
            phoneStateListener = listener
        }
    }

    private fun unregisterCallStateListener() {
        val telephonyManager = getSystemService<TelephonyManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let(telephonyManager::unregisterTelephonyCallback)
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION") telephonyManager.listen(
                phoneStateListener,
                PhoneStateListener.LISTEN_NONE
            )
            phoneStateListener = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class CallStateCallback : TelephonyCallback(),
        TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                serviceScope.launch {
                    stopSampling(
                        endReason = END_REASON_CALL_ENDED, requestedSource = CallSource.CELLULAR
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_START = "com.example.crowdmeasure.callsampling.START"
        const val ACTION_STOP = "com.example.crowdmeasure.callsampling.STOP"
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_CALL_SOURCE = "extra_call_source"
        private const val CHANNEL_ID = "call_cell_sampling"
        private const val NOTIFICATION_ID = 40_030
        private const val SAMPLE_INTERVAL_SECONDS = 5
        private const val END_REASON_CALL_ENDED = "call_ended"
        private const val RETENTION_MS = 7L * 24L * 60L * 60L * 1_000L

        fun requestStop(
            context: Context, callSource: CallSource = CallSource.UNKNOWN
        ) {
            runCatching {
                context.startService(
                    Intent(context, CallSamplingService::class.java).apply {
                        action = ACTION_STOP
                        putExtra(EXTRA_CALL_SOURCE, callSource.name)
                    })
            }
        }

        fun requestStart(
            context: Context, callType: CallType, callSource: CallSource
        ) {
            ContextCompat.startForegroundService(
                context, Intent(context, CallSamplingService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_CALL_TYPE, callType.name)
                    putExtra(EXTRA_CALL_SOURCE, callSource.name)
                })
        }
    }
}

private fun CallSource.isWhatsapp(): Boolean =
    this == CallSource.WHATSAPP_VOICE || this == CallSource.WHATSAPP_VIDEO || this == CallSource.WHATSAPP_UNKNOWN

private fun CallSource.matchesActiveSource(activeSource: CallSource): Boolean =
    this == CallSource.UNKNOWN || this == activeSource || (this.isWhatsapp() && activeSource.isWhatsapp())
