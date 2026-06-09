package com.yourcompany.crowdmeasure.sdk.upload.internal

import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.upload.InstallationIdProvider
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploader
import kotlinx.coroutines.sync.Mutex

internal data class InstalledUploadRuntime(
    val sdk: CrowdMeasureSdk,
    val uploader: MeasurementUploader,
    val installationIdProvider: InstallationIdProvider,
)

internal object UploadRuntime {
    @Volatile private var runtime: InstalledUploadRuntime? = null
    val mutex = Mutex()
    fun install(sdk: CrowdMeasureSdk, uploader: MeasurementUploader, provider: InstallationIdProvider) {
        runtime = InstalledUploadRuntime(sdk, uploader, provider)
    }
    fun get(): InstalledUploadRuntime? = runtime
}
