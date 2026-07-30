package com.crowdmeasure.sdk.background.internal

import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.background.BackgroundConfig
import kotlinx.coroutines.sync.Mutex

internal object BackgroundRuntime {
    @Volatile private var sdk: CrowdMeasureSdk? = null
    @Volatile private var config: BackgroundConfig? = null
    val runMutex = Mutex()

    @Synchronized
    fun install(value: CrowdMeasureSdk, valueConfig: BackgroundConfig) {
        val current = sdk
        if (current == null) {
            sdk = value
            config = valueConfig
        } else if (current !== value || config != valueConfig) {
            throw IllegalStateException("Background runtime is already installed with a different SDK instance")
        }
    }

    fun sdk(): CrowdMeasureSdk? = sdk
    fun config(): BackgroundConfig = config ?: BackgroundConfig()
}
