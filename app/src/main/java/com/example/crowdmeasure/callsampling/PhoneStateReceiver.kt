package com.example.crowdmeasure.callsampling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.media.AudioManager
import androidx.core.content.getSystemService
import android.telephony.TelephonyManager
import com.example.crowdmeasure.data.prefs.CallSamplingStatusStore
import com.example.crowdmeasure.domain.model.CallSource
import com.example.crowdmeasure.domain.model.CallType
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class PhoneStateReceiver : BroadcastReceiver() {
    @Inject
    lateinit var sessionRepository: UserSessionRepository

    @Inject
    lateinit var prerequisites: CallSamplingPrerequisites

    @Inject
    lateinit var statusStore: CallSamplingStatusStore

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {

            TelephonyManager.EXTRA_STATE_RINGING -> {
                wasRinging = true
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val callType = if (wasRinging) {
                    CallType.INCOMING
                } else {
                    CallType.OUTGOING
                }

                wasRinging = false

                handleOffHook(
                    context = context.applicationContext,
                    callType = callType
                )
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                wasRinging = false
                CallSamplingService.requestStop(
                    context = context.applicationContext,
                    callSource = CallSource.CELLULAR
                )
            }
        }
    }

    private fun handleOffHook(
        context: Context,
        callType: CallType
    ) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = sessionRepository.settings.first()
                delay(CELLULAR_START_CONFIRM_MS)
                val state = prerequisites.evaluate()

                if (!state.canStart) {
                    statusStore.recordMissedStart(state.missingReason)
                    return@launch
                }

                if (settings.voipCallSamplingEnabled &&
                    context.getSystemService<AudioManager>()?.mode.isVoipCommunicationMode()
                ) {
                    CallSamplingService.requestStart(
                        context,
                        callType = CallType.UNKNOWN,
                        callSource = CallSource.VOIP_GENERIC
                    )
                    return@launch
                }

                if (!settings.callSamplingEnabled) {
                    statusStore.recordMissedStart("call_sampling_disabled")
                    return@launch
                }

                try {
                    CallSamplingService.requestStart(
                        context,
                        callType = callType,
                        callSource = CallSource.CELLULAR
                    )
                } catch (error: Exception) {

                    val reason = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        error.javaClass.simpleName ==
                        "ForegroundServiceStartNotAllowedException"
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

    companion object {
        private const val CELLULAR_START_CONFIRM_MS = 2_000L
        private var wasRinging = false
    }
}
