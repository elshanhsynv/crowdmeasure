package com.example.crowdmeasure.callsampling

import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import com.example.crowdmeasure.data.prefs.CallSamplingStatusStore
import com.example.crowdmeasure.domain.model.CallSource
import com.example.crowdmeasure.domain.model.CallType
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoipCallMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionRepository: UserSessionRepository,
    private val prerequisites: CallSamplingPrerequisites,
    private val statusStore: CallSamplingStatusStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observeJob: Job? = null
    private var transitionJob: Job? = null
    private var voipDetected = false

    fun start() {
        if (observeJob?.isActive == true) return
        observeJob = scope.launch {
            sessionRepository.settings.collectLatest { settings ->
                if (voipDetected && !settings.voipCallSamplingEnabled) {
                    CallSamplingService.requestStop(context, CallSource.VOIP_GENERIC)
                }
                transitionJob?.cancel()
                transitionJob = null
                voipDetected = false
                statusStore.setVoipMonitorActive(settings.voipCallSamplingEnabled)
                if (settings.voipCallSamplingEnabled) monitorAudioMode()
            }
        }
    }

    private suspend fun monitorAudioMode() {
        while (scope.isActive) {
            evaluateAudioMode()
            delay(POLL_INTERVAL_MS)
        }
    }

    private fun evaluateAudioMode() {
        val mode = context.getSystemService<AudioManager>()?.mode
        val inCommunication = mode.isVoipCommunicationMode()
        Timber.tag(TAG).v("Audio mode=$mode, VoIP=$inCommunication")

        when {
            inCommunication && !voipDetected -> confirmTransition(START_CONFIRM_MS) {
                if (currentModeIsVoip()) {
                    voipDetected = true
                    requestStart()
                }
            }

            !inCommunication && voipDetected -> confirmTransition(END_CONFIRM_MS) {
                if (!currentModeIsVoip()) {
                    voipDetected = false
                    CallSamplingService.requestStop(context, CallSource.VOIP_GENERIC)
                }
            }

            else -> {
                transitionJob?.cancel()
                transitionJob = null
            }
        }
    }

    private fun confirmTransition(delayMs: Long, action: suspend () -> Unit) {
        if (transitionJob?.isActive == true) return
        transitionJob = scope.launch {
            delay(delayMs)
            action()
            transitionJob = null
        }
    }

    private fun currentModeIsVoip(): Boolean =
        context.getSystemService<AudioManager>()?.mode.isVoipCommunicationMode()

    private suspend fun requestStart() {
        val state = prerequisites.evaluate()
        if (!state.canStart) {
            statusStore.recordMissedStart("voip_${state.missingReason}")
            return
        }
        runCatching {
            CallSamplingService.requestStart(
                context = context,
                callType = CallType.UNKNOWN,
                callSource = CallSource.VOIP_GENERIC
            )
        }.onFailure { error ->
            statusStore.recordMissedStart(
                "voip_best_effort_start_failed:${error.javaClass.simpleName}"
            )
            Timber.tag(TAG).w(error, "Best-effort VoIP sampling start failed")
        }
    }

    companion object {
        private const val TAG = "VoipCallMonitor"
        private const val POLL_INTERVAL_MS = 1_000L
        private const val START_CONFIRM_MS = 2_000L
        private const val END_CONFIRM_MS = 3_000L
    }
}
