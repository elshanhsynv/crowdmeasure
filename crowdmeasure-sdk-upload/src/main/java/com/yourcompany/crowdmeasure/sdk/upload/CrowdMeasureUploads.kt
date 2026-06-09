package com.yourcompany.crowdmeasure.sdk.upload

import android.content.Context
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.upload.internal.DefaultInstallationIdProvider
import com.yourcompany.crowdmeasure.sdk.upload.internal.MeasurementUploadClientImpl
import com.yourcompany.crowdmeasure.sdk.upload.internal.UploadRuntime

object CrowdMeasureUploads {
    const val MIN_INTERVAL_MINUTES = 20L
    const val MAX_INTERVAL_MINUTES = 7L * 24L * 60L
    const val DEFAULT_INTERVAL_MINUTES = 60L

    fun install(
        context: Context,
        sdk: CrowdMeasureSdk,
        uploader: MeasurementUploader,
        installationIdProvider: InstallationIdProvider? = null,
    ): MeasurementUploadClient {
        val appContext = context.applicationContext
        UploadRuntime.install(
            sdk,
            uploader,
            installationIdProvider ?: DefaultInstallationIdProvider(appContext),
        )
        return MeasurementUploadClientImpl(appContext, sdk)
    }
}
