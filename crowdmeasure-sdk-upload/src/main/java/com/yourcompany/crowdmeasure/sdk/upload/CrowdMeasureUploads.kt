package com.crowdmeasure.sdk.upload

import android.content.Context
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.upload.internal.DefaultInstallationIdProvider
import com.crowdmeasure.sdk.upload.internal.MeasurementUploadClientImpl
import com.crowdmeasure.sdk.upload.internal.UploadRuntime

object CrowdMeasureUploads {
    const val MIN_INTERVAL_MINUTES = 20L
    const val MAX_INTERVAL_MINUTES = 7L * 24L * 60L
    const val DEFAULT_INTERVAL_MINUTES = 60L

    fun install(
        context: Context,
        sdk: CrowdMeasureSdk,
        uploader: MeasurementUploader,
        installationIdProvider: InstallationIdProvider? = null,
        config: MeasurementUploadConfig = MeasurementUploadConfig(),
    ): MeasurementUploadClient {
        val appContext = context.applicationContext
        UploadRuntime.install(
            sdk,
            uploader,
            installationIdProvider ?: DefaultInstallationIdProvider(appContext, config.preferencesName),
            config,
        )
        return MeasurementUploadClientImpl(appContext, sdk, config)
    }
}
