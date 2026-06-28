package com.crowdmeasure.sample

import android.app.Application
import com.crowdmeasure.sdk.CrowdMeasureConfig
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.crowdmeasure.sdk.background.CrowdMeasureBackground
import com.google.firebase.firestore.FirebaseFirestore
import com.crowdmeasure.sdk.firestore.measurements.CrowdMeasureFirestoreMeasurements
import com.crowdmeasure.sdk.firestore.calls.CrowdMeasureFirestoreCalls
import com.crowdmeasure.sdk.upload.CrowdMeasureUploads
import com.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.crowdmeasure.sdk.upload.MeasurementUploadError
import com.crowdmeasure.sdk.upload.MeasurementUploader
import com.crowdmeasure.sdk.upload.MeasurementUploaderResult
import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.CallSamplingConfig
import com.crowdmeasure.sdk.calls.CrowdMeasureCalls
import com.crowdmeasure.sdk.calls.upload.CallUploadClient
import com.crowdmeasure.sdk.calls.upload.CallUploadConfig
import com.crowdmeasure.sdk.calls.upload.CrowdMeasureCallUploads
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
    lateinit var callUploads: CallUploadClient
        private set

    override fun onCreate() {
        super.onCreate()
        sdk = CrowdMeasureSdk.create(this)
        background = CrowdMeasureBackground.install(this, sdk)
        val uploader = runCatching {
            CrowdMeasureFirestoreMeasurements.create(FirebaseFirestore.getInstance())
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
        )
        callUploads = CrowdMeasureCallUploads.install(
            this,
            calls,
            CallUploadConfig(
                uploader = CrowdMeasureFirestoreCalls.create(FirebaseFirestore.getInstance()),
            ),
        )
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            calls.activateEnabledFeatures()
        }
    }
}
