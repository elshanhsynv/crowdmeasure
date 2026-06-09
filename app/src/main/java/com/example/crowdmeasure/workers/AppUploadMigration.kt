package com.example.crowdmeasure.workers

import android.content.Context
import androidx.work.WorkManager
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUploadMigration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val uploads: MeasurementUploadClient,
) {
    suspend fun migrateOnce() {
        if (preferences.isSdkUploadMigrated()) return
        WorkManager.getInstance(context).cancelUniqueWork("upload_pending_measurements")
        val settings = preferences.settingsFirst()
        val result = if (settings.firestoreUploadsEnabled) {
            uploads.enable(intervalMinutes = 60, wifiOnly = true)
        } else {
            uploads.disable()
        }
        if (result is MeasurementUploadResult.Success) preferences.markSdkUploadMigrated()
    }
}
