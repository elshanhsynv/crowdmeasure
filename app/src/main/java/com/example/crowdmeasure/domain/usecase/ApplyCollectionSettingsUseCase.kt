package com.example.crowdmeasure.domain.usecase

import com.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.crowdmeasure.sdk.background.BackgroundResult
import com.crowdmeasure.sdk.background.CrowdMeasureBackground
import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.CallSamplingResult
import com.crowdmeasure.sdk.calls.upload.CallUploadClient
import com.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.crowdmeasure.sdk.upload.MeasurementUploadResult
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject

/**
 * The single place that turns SDK features on or off. The app settings are the user's intent
 * (on by default); they are only applied while the current consent version is accepted.
 * Without consent every feature is switched off, whatever the SDK defaults are.
 */
class ApplyCollectionSettingsUseCase @Inject constructor(
    private val session: UserSessionRepository,
    private val background: BackgroundCollectionClient,
    private val uploads: MeasurementUploadClient,
    private val calls: CallSamplingClient,
    private val callUploads: CallUploadClient,
) {
    suspend operator fun invoke() {
        val settings = session.settings.first()
        val consented = settings.consentGateDismissed

        val backgroundResult = if (consented && settings.autoRunEnabled) {
            background.enable(
                settings.autoRunIntervalMinutes.toLong().coerceIn(
                    CrowdMeasureBackground.MIN_INTERVAL_MINUTES,
                    CrowdMeasureBackground.MAX_INTERVAL_MINUTES,
                ),
                settings.collectOnlyWifi,
            )
        } else {
            background.disable()
        }
        if (backgroundResult is BackgroundResult.Failure) {
            Timber.w("Background collection update failed: %s", backgroundResult.error)
        }

        val uploadsOn = consented && settings.firestoreUploadsEnabled
        val uploadResult = if (uploadsOn) {
            uploads.enable(UPLOAD_INTERVAL_MINUTES, wifiOnly = false)
        } else {
            uploads.disable()
        }
        if (uploadResult is MeasurementUploadResult.Failure) {
            Timber.w("Measurement upload update failed: %s", uploadResult.error)
        }
        val callUploadResult = if (uploadsOn) {
            callUploads.enable(UPLOAD_INTERVAL_MINUTES, wifiOnly = true)
        } else {
            callUploads.disable()
        }

        val callResults = listOf(
            callUploadResult,
            calls.setCellularSamplingEnabled(consented && settings.callSamplingEnabled),
            calls.setVoipSamplingEnabled(consented && settings.voipCallSamplingEnabled),
            calls.activateEnabledFeatures(),
        )
        callResults.filterIsInstance<CallSamplingResult.Failure>().forEach {
            Timber.w("Call sampling update failed: %s", it.error)
        }
    }

    private companion object {
        const val UPLOAD_INTERVAL_MINUTES = 60L
    }
}
