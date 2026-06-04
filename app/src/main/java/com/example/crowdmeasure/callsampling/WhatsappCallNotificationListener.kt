package com.example.crowdmeasure.callsampling

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.crowdmeasure.data.prefs.CallSamplingStatusStore
import com.example.crowdmeasure.domain.model.CallSource
import com.example.crowdmeasure.domain.model.CallType
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class WhatsappCallNotificationListener : NotificationListenerService() {
    @Inject
    lateinit var sessionRepository: UserSessionRepository

    @Inject
    lateinit var prerequisites: CallSamplingPrerequisites

    @Inject
    lateinit var statusStore: CallSamplingStatusStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var debounceJob: Job? = null
    private var activeSource: CallSource? = null

    override fun onListenerConnected() {
        scheduleEvaluate()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName in WHATSAPP_PACKAGES) {
            scheduleEvaluate()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName in WHATSAPP_PACKAGES) {
            scheduleEvaluate()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun scheduleEvaluate() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(NOTIFICATION_DEBOUNCE_MS.milliseconds)
            evaluateActiveNotifications()
        }
    }

    private suspend fun evaluateActiveNotifications() {
        val source = runCatching {
            activeNotifications
                .asSequence().firstNotNullOfOrNull { it.toWhatsappCallSource() }
        }.getOrNull()

        if (source == null) {
            CallSamplingService.requestStop(
                context = applicationContext,
                callSource = activeSource ?: CallSource.WHATSAPP_UNKNOWN
            )
            activeSource = null
            Timber.tag("WhatsappCallSampling").i("No active WhatsApp call notifications found, stopping sampling")
            return
        }

        if (activeSource?.isWhatsapp() == true) return

        val settings = sessionRepository.settings.first()
        if (!settings.whatsappCallSamplingEnabled) {
            statusStore.recordMissedStart("whatsapp_sampling_disabled")
            Timber.tag("WhatsappCallSampling").i(
                "WhatsApp call sampling is disabled in settings, not starting for source: $source"
            )
            return
        }

        val state = prerequisites.evaluate()
        if (!state.canStart) {
            statusStore.recordMissedStart("whatsapp_${state.missingReason}")
            Timber.tag("WhatsappCallSampling").i(
                "Cannot start call sampling for source: $source, reason: ${state.missingReason}"
            )
            return
        }

        try {
            CallSamplingService.requestStart(
                context = applicationContext,
                callType = CallType.UNKNOWN,
                callSource = source
            )
            activeSource = source
            Timber.tag("WhatsappCallSampling").i("Started call sampling for source: $source")
        } catch (error: Exception) {
            statusStore.recordMissedStart(
                "whatsapp_foreground_service_start_failed:${error.javaClass.simpleName}"
            )
            Timber.tag("WhatsappCallSampling").w(error, "Failed to start call sampling service")
        }
    }

    private fun StatusBarNotification.toWhatsappCallSource(): CallSource? {
        if (packageName !in WHATSAPP_PACKAGES) return null

        val notification = notification ?: return null
        val text = notification.searchableText()
        val lowerText = text.lowercase()
        val isCallCategory = notification.category == Notification.CATEGORY_CALL
        val isOngoing = isOngoing || notification.isOngoingFlagSet()
        val looksLikeCall = isCallCategory || CALL_WORDS.any(lowerText::contains)
        val looksMissed = MISSED_WORDS.any(lowerText::contains)
        val looksRinging = RINGING_ACTION_WORDS.any(lowerText::contains)

        if (!isOngoing || !looksLikeCall || looksMissed || looksRinging) return null

        return when {
            VIDEO_WORDS.any(lowerText::contains) -> {
                Timber.tag("WhatsappCallSampling").i(
                    "Identified WhatsApp video call notification with text: $text"
                )
                CallSource.WHATSAPP_VIDEO
            }
            VOICE_WORDS.any(lowerText::contains) -> {
                Timber.tag("WhatsappCallSampling").i(
                    "Identified WhatsApp voice call notification with text: $text"
                )
                CallSource.WHATSAPP_VOICE
            }
            else -> {
                Timber.tag("WhatsappCallSampling").i(
                    "Identified WhatsApp call notification with unknown type, text: $text"
                )
                CallSource.WHATSAPP_UNKNOWN
            }
        }
    }

    private fun Notification.searchableText(): String {
        val parts = buildList {
            extras.getCharSequence(Notification.EXTRA_TITLE)?.let { add(it.toString()) }
            extras.getCharSequence(Notification.EXTRA_TEXT)?.let { add(it.toString()) }
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { add(it.toString()) }
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { add(it.toString()) }
            extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.let { add(it.toString()) }
            actions?.forEach { action -> add(action.title?.toString().orEmpty()) }
            category?.let(::add)
        }
        return parts.joinToString(separator = " ")
    }

    private fun Notification.isOngoingFlagSet(): Boolean =
        flags and Notification.FLAG_ONGOING_EVENT != 0

    companion object {
        private const val NOTIFICATION_DEBOUNCE_MS = 750L

        private val WHATSAPP_PACKAGES = setOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        )

        private val CALL_WORDS = listOf("call", "calling", "voice", "video")
        private val VOICE_WORDS = listOf("voice", "audio")
        private val VIDEO_WORDS = listOf("video")
        private val MISSED_WORDS = listOf("missed")
        private val RINGING_ACTION_WORDS = listOf("answer", "decline", "incoming")
    }
}

private fun CallSource.isWhatsapp(): Boolean =
    this == CallSource.WHATSAPP_VOICE ||
        this == CallSource.WHATSAPP_VIDEO ||
        this == CallSource.WHATSAPP_UNKNOWN
