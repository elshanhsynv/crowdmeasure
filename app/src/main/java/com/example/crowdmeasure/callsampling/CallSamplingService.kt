package com.example.crowdmeasure.callsampling

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
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
import com.example.crowdmeasure.domain.repo.UserSessionRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    lateinit var workScheduler: WorkScheduler
    @Inject
    lateinit var sessionRepository: UserSessionRepository
    @Inject
    @IoDispatcher
    lateinit var io: CoroutineDispatcher

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val recoveryMutex = Mutex()
    private var recovered = false
    private var samplingJob: Job? = null
    private var sourceCorrectionJob: Job? = null
    private var activeSession: CallSession? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var foregroundStarted = false
    private var callType = CallType.UNKNOWN
    private var callSource = CallSource.UNKNOWN

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            serviceScope.launch {
                ensureRecovered()
                stopServiceIfIdle()
            }
            return START_NOT_STICKY
        }

        if (intent.action == ACTION_START) {
            if (!tryStartInForeground(intent.action.orEmpty())) return START_NOT_STICKY
        }
        val requestedType = intent.callType()
        val requestedSource = intent.callSource()
        serviceScope.launch {
            ensureRecovered()
            when (intent.action) {
                ACTION_START -> startSampling(requestedType, requestedSource)
                ACTION_STOP -> stopSampling(END_REASON_CALL_ENDED, requestedSource)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterCallStateListener()
        sourceCorrectionJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun ensureRecovered() {
        recoveryMutex.withLock {
            if (recovered) return
            repository.finishActiveSession(System.currentTimeMillis(), END_REASON_SERVICE_RESTARTED)
            recovered = true
        }
    }

    private suspend fun reclassifyActiveSessionAsVoip() {
        val session = activeSession ?: return
        repository.reclassifySession(
            sessionId = session.sessionId,
            callType = CallType.UNKNOWN,
            callSource = CallSource.VOIP_GENERIC
        )
        unregisterCallStateListener()
        callType = CallType.UNKNOWN
        callSource = CallSource.VOIP_GENERIC
        activeSession = session.copy(
            callType = CallType.UNKNOWN,
            callSource = CallSource.VOIP_GENERIC
        )
        updateForeground()
        Timber.tag(TAG).i("Reclassified active cellular session as generic VoIP")
    }

    private suspend fun startSampling(requestedType: CallType, requestedSource: CallSource) {
        if (requestedSource == CallSource.UNKNOWN) return
        if (activeSession != null) {
            if (requestedSource == CallSource.VOIP_GENERIC && callSource == CallSource.CELLULAR) {
                reclassifyActiveSessionAsVoip()
                return
            }
            if (callSource == requestedSource || callSource == CallSource.CELLULAR) return
            stopSampling("replaced_by_${requestedSource.name.lowercase()}", callSource)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            statusStore.recordMissedStart("android_q_required")
            stopServiceIfIdle()
            return
        }

        callType = requestedType
        callSource = requestedSource
        startInForeground()
        repository.deleteOlderThan(System.currentTimeMillis() - RETENTION_MS)
        if (callSource == CallSource.CELLULAR) registerCallStateListener()
        if (callSource == CallSource.CELLULAR &&
            sessionRepository.settings.first().voipCallSamplingEnabled
        ) {
            startSourceCorrectionChecks()
        }
        val session = repository.startSession(callType, callSource, SAMPLE_INTERVAL_SECONDS)
        activeSession = session
        updateForeground()
        samplingJob = serviceScope.launch {
            while (isActive) {
                collectSample(session)
                delay((SAMPLE_INTERVAL_SECONDS * 1_000L).milliseconds)
            }
        }
        Timber.tag(TAG).i("Started ${callSource.name} call sampling")
    }

    private suspend fun stopSampling(endReason: String, requestedSource: CallSource) {
        if (!requestedSource.matchesActiveSource(callSource)) return
        val session = activeSession ?: return
        samplingJob?.cancelAndJoin()
        samplingJob = null
        sourceCorrectionJob?.cancelAndJoin()
        sourceCorrectionJob = null
        unregisterCallStateListener()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) collectSample(session)
        val now = System.currentTimeMillis()
        repository.finishSession(session.sessionId, now, endReason)
        repository.deleteOlderThan(now - RETENTION_MS)
        workScheduler.kickoffCallUploadOnce()
        activeSession = null
        callType = CallType.UNKNOWN
        callSource = CallSource.UNKNOWN

        stopServiceIfIdle()
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
                cellInfo = cell
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.tag(TAG).w(error, "Call cell sample failed")
        }
    }

    private fun startInForeground() {
        updateForeground()
        foregroundStarted = true
    }

    private fun tryStartInForeground(reason: String): Boolean =
        runCatching {
            startInForeground()
            true
        }.getOrElse { error ->
            Timber.tag(TAG).e(error, "Unable to start call sampling foreground service")
            serviceScope.launch {
                statusStore.recordMissedStart(
                    "foreground_service_failed:$reason:${error.javaClass.simpleName}"
                )
            }
            stopSelf()
            false
        }

    private fun updateForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
        foregroundStarted = true
    }

    private fun stopServiceIfIdle() {
        if (activeSession != null) return
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.crowdmeasure)
            .setContentTitle("Measuring signal quality")
            .setContentText("Collecting network stats during this call.")
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Call cell sampling", NotificationManager.IMPORTANCE_LOW
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun registerCallStateListener() {
        val telephonyManager = getSystemService<TelephonyManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = CallStateCallback()
            telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            telephonyCallback = callback
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated by Android framework")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) {
                        serviceScope.launch {
                            stopSampling(END_REASON_CALL_ENDED, CallSource.CELLULAR)
                        }
                    }
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            phoneStateListener = listener
        }
    }

    private fun startSourceCorrectionChecks() {
        sourceCorrectionJob?.cancel()
        sourceCorrectionJob = serviceScope.launch {
            var consecutiveVoipReads = 0
            while (isActive && callSource == CallSource.CELLULAR) {
                val inCommunication =
                    getSystemService<AudioManager>()?.mode.isVoipCommunicationMode()
                consecutiveVoipReads = if (inCommunication) consecutiveVoipReads + 1 else 0
                if (consecutiveVoipReads >= REQUIRED_VOIP_READS) {
                    reclassifyActiveSessionAsVoip()
                    return@launch
                }
                delay(SOURCE_CHECK_INTERVAL_MS.milliseconds)
            }
        }
    }

    private fun unregisterCallStateListener() {
        val telephonyManager = getSystemService<TelephonyManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let(telephonyManager::unregisterTelephonyCallback)
            telephonyCallback = null
        } else {
            @Suppress("DEPRECATION")
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            phoneStateListener = null
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private inner class CallStateCallback : TelephonyCallback(),
        TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            if (state == TelephonyManager.CALL_STATE_IDLE) {
                serviceScope.launch { stopSampling(END_REASON_CALL_ENDED, CallSource.CELLULAR) }
            }
        }
    }

    companion object {
        const val ACTION_START = "com.example.crowdmeasure.callsampling.START"
        const val ACTION_STOP = "com.example.crowdmeasure.callsampling.STOP"
        const val EXTRA_CALL_TYPE = "extra_call_type"
        const val EXTRA_CALL_SOURCE = "extra_call_source"
        private const val TAG = "CallSamplingService"
        private const val CHANNEL_ID = "call_cell_sampling"
        private const val NOTIFICATION_ID = 40_030
        private const val SAMPLE_INTERVAL_SECONDS = 5
        private const val SOURCE_CHECK_INTERVAL_MS = 1_000L
        private const val REQUIRED_VOIP_READS = 2
        private const val END_REASON_CALL_ENDED = "call_ended"
        private const val END_REASON_SERVICE_RESTARTED = "service_restarted"
        private const val RETENTION_MS = 7L * 24L * 60L * 60L * 1_000L

        fun requestStop(context: Context, callSource: CallSource = CallSource.UNKNOWN) {
            runCatching {
                context.startService(
                    serviceIntent(
                        context,
                        ACTION_STOP,
                        CallType.UNKNOWN,
                        callSource
                    )
                )
            }
        }

        fun requestStart(context: Context, callType: CallType, callSource : CallSource) {
            ContextCompat.startForegroundService(
                context, serviceIntent(context, ACTION_START, callType, callSource)
            )
        }

        private fun serviceIntent(
            context: Context,
            action: String,
            callType: CallType = CallType.UNKNOWN,
            callSource: CallSource = CallSource.UNKNOWN
        ) = Intent(context, CallSamplingService::class.java).apply {
            this.action = action
            putExtra(EXTRA_CALL_TYPE, callType.name)
            putExtra(EXTRA_CALL_SOURCE, callSource.name)
        }
    }
}

internal fun Int?.isVoipCommunicationMode(): Boolean =
    this == AudioManager.MODE_IN_COMMUNICATION ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    this == AudioManager.MODE_COMMUNICATION_REDIRECT)

private fun Intent.callType(): CallType =
    getStringExtra(CallSamplingService.EXTRA_CALL_TYPE)
        ?.let { runCatching { CallType.valueOf(it) }.getOrNull() }
        ?: CallType.UNKNOWN

private fun Intent.callSource(): CallSource =
    getStringExtra(CallSamplingService.EXTRA_CALL_SOURCE)
        ?.let { runCatching { CallSource.valueOf(it) }.getOrNull() }
        ?: CallSource.UNKNOWN

private fun CallSource.isWhatsapp(): Boolean =
    this == CallSource.WHATSAPP_VOICE ||
            this == CallSource.WHATSAPP_VIDEO ||
            this == CallSource.WHATSAPP_UNKNOWN

private fun CallSource.matchesActiveSource(activeSource: CallSource): Boolean =
    this == CallSource.UNKNOWN ||
            this == activeSource ||
            (this.isWhatsapp() && activeSource.isWhatsapp())
