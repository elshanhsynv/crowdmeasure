package com.crowdmeasure.sdk.calls

import android.content.Context
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.calls.internal.*

object CrowdMeasureCalls {
    fun install(
        context: Context,
        sdk: CrowdMeasureSdk,
        config: CallSamplingConfig,
        callStore: CallStore? = null,
    ): CallSamplingClient {
        require(config.notificationIconResId != 0) { "notificationIconResId is required" }
        require(config.databaseName.isNotBlank()) { "databaseName must not be blank" }
        require(config.sampleIntervalSeconds in 1..300) { "sampleIntervalSeconds must be between 1 and 300" }
        require(config.retentionDays in 1..365) { "retentionDays must be between 1 and 365" }
        val appContext = context.applicationContext
        val settings = CallsSettingsStore(appContext, config.preferencesName)
        val monitor = VoipCallMonitor(appContext, settings)
        CallsRuntime.install(
            InstalledCallsRuntime(
                appContext,
                sdk,
                config,
                callStore ?: DefaultCallStore.create(appContext, config.databaseName),
                settings,
                monitor,
            )
        )
        return CallSamplingClientImpl(appContext)
    }
}
