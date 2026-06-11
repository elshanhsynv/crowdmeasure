package com.example.crowdmeasure.workers

import android.content.Context
import androidx.work.WorkManager
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.CallSamplingResult
import com.crowdmeasure.sdk.calls.upload.CallUploadClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppCallsMigration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val calls: CallSamplingClient,
    private val callUploads: CallUploadClient,
) {
    suspend fun migrateOnce() {
        if (preferences.isSdkCallsMigrated()) return
        WorkManager.getInstance(context).cancelUniqueWork("upload_pending_calls")
        WorkManager.getInstance(context).cancelUniqueWork("upload_pending_calls_kickoff")
        WorkManager.getInstance(context).cancelUniqueWork("reschedule_background_work")
        val settings = preferences.settingsFirst()
        val cellular = calls.setCellularSamplingEnabled(settings.callSamplingEnabled)
        val voip = calls.setVoipSamplingEnabled(settings.voipCallSamplingEnabled)
        val uploads = if (settings.firestoreUploadsEnabled) callUploads.enable(60, true) else callUploads.disable()
        if (cellular is CallSamplingResult.Success &&
            voip is CallSamplingResult.Success &&
            uploads is CallSamplingResult.Success
        ) preferences.markSdkCallsMigrated()
    }
}
