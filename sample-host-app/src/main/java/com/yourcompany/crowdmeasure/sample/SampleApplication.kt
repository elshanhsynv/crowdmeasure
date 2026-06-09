package com.yourcompany.crowdmeasure.sample

import android.app.Application
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureConfig
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.yourcompany.crowdmeasure.sdk.background.CrowdMeasureBackground
import com.google.firebase.firestore.FirebaseFirestore
import com.yourcompany.crowdmeasure.sdk.firestore.CrowdMeasureFirestore
import com.yourcompany.crowdmeasure.sdk.upload.CrowdMeasureUploads
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadError
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploader
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploaderResult
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingClient
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingConfig
import com.yourcompany.crowdmeasure.sdk.calls.CrowdMeasureCalls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SampleApplication : Application() {
    lateinit var sdk: CrowdMeasureSdk
        private set
    lateinit var background: BackgroundCollectionClient
        private set
    lateinit var uploads: MeasurementUploadClient
        private set
    lateinit var calls: CallSamplingClient
        private set

    override fun onCreate() {
        super.onCreate()
        sdk = CrowdMeasureSdk.create(this, CrowdMeasureConfig(loggingEnabled = true))
        background = CrowdMeasureBackground.install(this, sdk)
        val uploader = runCatching {
            CrowdMeasureFirestore.create(FirebaseFirestore.getInstance())
        }.getOrElse { initError ->
            MeasurementUploader { _ ->
                MeasurementUploaderResult.Failure(MeasurementUploadError.BackendRejected(initError))
            }
        }
        uploads = CrowdMeasureUploads.install(this, sdk, uploader)
        calls = CrowdMeasureCalls.install(
            context = this,
            sdk = sdk,
            config = CallSamplingConfig(
                notificationIconResId = R.drawable.crowdmeasure_sample_notification,
            ),
            uploader = runCatching { CrowdMeasureFirestore.createCallUploader(FirebaseFirestore.getInstance()) }
                .getOrNull(),
        )
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            calls.activateEnabledFeatures()
        }
    }
}
