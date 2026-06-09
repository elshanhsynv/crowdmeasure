package com.yourcompany.crowdmeasure.sdk.background.internal

import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import kotlinx.coroutines.sync.Mutex

internal object BackgroundRuntime {
    @Volatile private var sdk: CrowdMeasureSdk? = null
    val runMutex = Mutex()

    fun install(value: CrowdMeasureSdk) {
        sdk = value
    }

    fun sdk(): CrowdMeasureSdk? = sdk
}
