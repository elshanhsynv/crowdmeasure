package com.crowdmeasure.sdk.background

import android.content.Context
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.background.internal.BackgroundClientImpl
import com.crowdmeasure.sdk.background.internal.BackgroundRuntime

object CrowdMeasureBackground {
    const val MIN_INTERVAL_MINUTES = 20L
    const val MAX_INTERVAL_MINUTES = 7L * 24L * 60L
    const val DEFAULT_INTERVAL_MINUTES = 60L

    fun install(
        context: Context,
        sdk: CrowdMeasureSdk,
        config: BackgroundConfig = BackgroundConfig(),
    ): BackgroundCollectionClient {
        val appContext = context.applicationContext
        BackgroundRuntime.install(sdk, config)
        return BackgroundClientImpl(appContext, config)
    }
}
