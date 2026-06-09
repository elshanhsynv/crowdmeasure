package com.yourcompany.crowdmeasure.sdk.calls

import android.content.Context
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.calls.internal.*

object CrowdMeasureCalls {
    const val MIN_UPLOAD_INTERVAL_MINUTES = 20L
    const val MAX_UPLOAD_INTERVAL_MINUTES = 7L * 24 * 60

    fun install(
        context: Context,
        sdk: CrowdMeasureSdk,
        config: CallSamplingConfig,
        uploader: CallUploader? = null,
        callStore: CallStore? = null,
        installationIdProvider: CallInstallationIdProvider? = null,
    ): CallSamplingClient {
        require(config.notificationIconResId != 0) { "notificationIconResId is required" }
        require(config.databaseName.isNotBlank()) { "databaseName must not be blank" }
        require(config.sampleIntervalSeconds in 1..300) { "sampleIntervalSeconds must be between 1 and 300" }
        require(config.retentionDays in 1..365) { "retentionDays must be between 1 and 365" }
        val appContext = context.applicationContext
        val settings = CallsSettingsStore(appContext)
        val monitor = VoipCallMonitor(appContext, settings)
        CallsRuntime.install(
            InstalledCallsRuntime(
                appContext,
                sdk,
                config,
                callStore ?: DefaultCallStore.create(appContext, config.databaseName),
                uploader,
                installationIdProvider ?: CallInstallationIdProvider { settings.installationId() },
                settings,
                monitor,
            )
        )
        return CallSamplingClientImpl(appContext)
    }
}
