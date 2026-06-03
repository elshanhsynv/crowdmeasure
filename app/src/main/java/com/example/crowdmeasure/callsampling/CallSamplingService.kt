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
import androidx.core.content.getSystemService
import com.example.crowdmeasure.R
import com.example.crowdmeasure.data.measurement.collectors.TelephonyCollector
import com.example.crowdmeasure.data.prefs.CallSamplingStatusStore
import com.example.crowdmeasure.di.IoDispatcher
import com.example.crowdmeasure.domain.model.CallSession
import com.example.crowdmeasure.domain.repo.CallSamplingRepository
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

@AndroidEntryPoint
class CallSamplingService : Service() {
    @Inject
    lateinit var repository: CallSamplingRepository
    @Inject
    lateinit var statusStore: CallSamplingStatusStore
    @Inject
    @IoDispatcher
    lateinit var io: CoroutineDispatcher
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var samplingJob: Job? = null
    private var activeSession: CallSession? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null
    private var foregroundStarted = false

    private var cType: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerCallStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callType = intent
            ?.getStringExtra(EXTRA_CALL_TYPE)
        cType = callType

        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch {
                    stopSampling(END_REASON_CALL_ENDED)
                }
            }

            else -> {
                startInForeground()
                serviceScope.launch {
                    Timber.tag("CallSamplingService").d("Started sampling during Call...")
                    startSampling()
                    Timber.tag("CallSamplingService").d("Call Type = $callType")
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
        val session = repository.startSession(SAMPLE_INTERVAL_SECONDS)
        activeSession = session

        samplingJob = serviceScope.launch {
            while (isActive) {
                collectSample(session)
                delay(SAMPLE_INTERVAL_SECONDS * 1_000L)
            }
        }
    }

    private suspend fun stopSampling(endReason: String) {
        val session = activeSession
        samplingJob?.cancelAndJoin()
        samplingJob = null

        if (session != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                collectSample(session)
            }
            val now = System.currentTimeMillis()
            repository.finishSession(session.sessionId, now, endReason)
            repository.deleteOlderThan(now - RETENTION_MS)
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
            this,
            NOTIFICATION_ID,
            buildNotification(),
            foregroundType
        )
        foregroundStarted = true
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.crowdmeasure)
            .setContentTitle("Collecting call cell stats")
            .setContentText("Sampling cellular signal during the active call.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call cell sampling",
            NotificationManager.IMPORTANCE_LOW
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
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated by Android framework")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state == TelephonyManager.CALL_STATE_IDLE) {
                        serviceScope.launch { stopSampling(END_REASON_CALL_ENDED) }
                    }
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            phoneStateListener = listener
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
                serviceScope.launch { stopSampling(END_REASON_CALL_ENDED) }
            }
        }
    }

    companion object {
        const val ACTION_START = "com.example.crowdmeasure.callsampling.START"
        const val ACTION_STOP = "com.example.crowdmeasure.callsampling.STOP"
        const val EXTRA_CALL_TYPE = "extra_call_type"
        private const val CHANNEL_ID = "call_cell_sampling"
        private const val NOTIFICATION_ID = 40_030
        private const val SAMPLE_INTERVAL_SECONDS = 30
        private const val END_REASON_CALL_ENDED = "call_ended"
        private const val RETENTION_MS = 7L * 24L * 60L * 60L * 1_000L

        fun requestStop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, CallSamplingService::class.java).apply {
                        action = ACTION_STOP
                    }
                )
            }
        }
    }
}
