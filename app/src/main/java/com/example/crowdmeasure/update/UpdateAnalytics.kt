package com.example.crowdmeasure.update

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateAnalytics @Inject constructor(
    private val analytics: FirebaseAnalytics,
    private val crashlytics: FirebaseCrashlytics
) {
    fun available(metadata: UpdateMetadata) {
        analytics.logEvent("update_available", metadataBundle(metadata))
    }

    fun downloadStarted(metadata: UpdateMetadata) {
        analytics.logEvent("update_download_started", metadataBundle(metadata))
    }

    fun downloadVerified(metadata: UpdateMetadata) {
        analytics.logEvent("update_download_verified", metadataBundle(metadata))
    }

    fun installOpened(metadata: UpdateMetadata) {
        analytics.logEvent("update_install_opened", metadataBundle(metadata))
    }

    fun failed(stage: String, metadata: UpdateMetadata?, throwable: Throwable) {
        analytics.logEvent(
            "update_failed",
            metadataBundle(metadata).apply {
                putString("stage", stage)
                putString("error", throwable.javaClass.simpleName)
            }
        )
        crashlytics.recordException(throwable)
    }

    private fun metadataBundle(metadata: UpdateMetadata?): Bundle = Bundle().apply {
        if (metadata != null) {
            putInt("target_version_code", metadata.versionCode)
            putString("target_version_name", metadata.versionName.orEmpty())
            putBoolean("force_update", metadata.forceUpdate)
        }
    }
}
