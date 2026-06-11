package com.crowdmeasure.sdk.calls.upload.internal

import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.upload.CallUploadConfig
import kotlinx.coroutines.sync.Mutex

internal data class InstalledCallUploadRuntime(
    val calls: CallSamplingClient,
    val config: CallUploadConfig,
)

internal object CallUploadRuntime {
    @Volatile private var runtime: InstalledCallUploadRuntime? = null
    val mutex = Mutex()

    @Synchronized
    fun install(value: InstalledCallUploadRuntime) {
        val current = runtime
        if (current == null) runtime = value
        else if (current.calls !== value.calls || current.config != value.config) {
            throw IllegalStateException("Call upload runtime is already installed with a different configuration")
        }
    }

    fun get(): InstalledCallUploadRuntime? = runtime
}
