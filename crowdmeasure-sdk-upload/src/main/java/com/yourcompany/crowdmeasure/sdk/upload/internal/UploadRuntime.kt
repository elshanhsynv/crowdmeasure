package com.crowdmeasure.sdk.upload.internal

import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.upload.InstallationIdProvider
import com.crowdmeasure.sdk.upload.MeasurementUploader
import com.crowdmeasure.sdk.upload.MeasurementUploadConfig
import kotlinx.coroutines.sync.Mutex

internal data class InstalledUploadRuntime(
    val sdk: CrowdMeasureSdk,
    val uploader: MeasurementUploader,
    val installationIdProvider: InstallationIdProvider,
    val config: MeasurementUploadConfig,
)

internal object UploadRuntime {
    @Volatile private var runtime: InstalledUploadRuntime? = null
    val mutex = Mutex()
    @Synchronized
    fun install(sdk: CrowdMeasureSdk, uploader: MeasurementUploader, provider: InstallationIdProvider, config: MeasurementUploadConfig) {
        val current = runtime
        if (current == null) runtime = InstalledUploadRuntime(sdk, uploader, provider, config)
        else if (current.sdk !== sdk || current.uploader !== uploader || current.config != config) {
            throw IllegalStateException("Measurement upload runtime is already installed with a different configuration")
        }
    }
    fun get(): InstalledUploadRuntime? = runtime
}
