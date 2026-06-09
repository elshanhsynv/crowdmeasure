package com.yourcompany.crowdmeasure.sdk.background

import android.content.Context
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.background.internal.BackgroundClientImpl
import com.yourcompany.crowdmeasure.sdk.background.internal.BackgroundRuntime

object CrowdMeasureBackground {
    const val MIN_INTERVAL_MINUTES = 20L
    const val MAX_INTERVAL_MINUTES = 7L * 24L * 60L
    const val DEFAULT_INTERVAL_MINUTES = 60L

    fun install(context: Context, sdk: CrowdMeasureSdk): BackgroundCollectionClient {
        val appContext = context.applicationContext
        BackgroundRuntime.install(sdk)
        return BackgroundClientImpl(appContext)
    }
}
