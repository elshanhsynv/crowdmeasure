package com.example.crowdmeasure.callsampling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.crowdmeasure.data.prefs.CallSamplingStatusStore
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhoneStateReceiver : BroadcastReceiver() {

    @Inject lateinit var sessionRepository: UserSessionRepository
    @Inject lateinit var prerequisites: CallSamplingPrerequisites
    @Inject lateinit var statusStore: CallSamplingStatusStore

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> handleOffHook(context.applicationContext)
            TelephonyManager.EXTRA_STATE_IDLE -> CallSamplingService.requestStop(context.applicationContext)
        }
    }

    private fun handleOffHook(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = sessionRepository.settings.first()
                if (!settings.callSamplingEnabled) {
                    statusStore.recordMissedStart("call_sampling_disabled")
                    return@launch
                }

                val state = prerequisites.evaluate()
                if (!state.canStart) {
                    statusStore.recordMissedStart(state.missingReason)
                    return@launch
                }

                try {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, CallSamplingService::class.java).apply {
                            action = CallSamplingService.ACTION_START
                        }
                    )
                } catch (error: Exception) {
                    val reason = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        error.javaClass.simpleName == "ForegroundServiceStartNotAllowedException"
                    ) {
                        "foreground_service_start_not_allowed"
                    } else {
                        "foreground_service_start_failed:${error.javaClass.simpleName}"
                    }
                    statusStore.recordMissedStart(reason)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
