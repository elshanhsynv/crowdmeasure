package com.crowdmeasure.sdk.calls.upload

import android.content.Context
import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.upload.internal.CallUploadClientImpl
import com.crowdmeasure.sdk.calls.upload.internal.CallUploadRuntime
import com.crowdmeasure.sdk.calls.upload.internal.InstalledCallUploadRuntime

object CrowdMeasureCallUploads {
    fun install(
        context: Context,
        calls: CallSamplingClient,
        config: CallUploadConfig,
    ): CallUploadClient {
        val appContext = context.applicationContext
        CallUploadRuntime.install(InstalledCallUploadRuntime(calls, config))
        return CallUploadClientImpl(appContext, config)
    }
}
