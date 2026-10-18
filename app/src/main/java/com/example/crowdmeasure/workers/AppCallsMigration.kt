package com.example.crowdmeasure.workers

import android.content.Context
import androidx.work.WorkManager
import com.example.crowdmeasure.data.prefs.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cancels pre-SDK call work. Feature state is applied by
 * [com.example.crowdmeasure.domain.usecase.ApplyCollectionSettingsUseCase].
 */
@Singleton
class AppCallsMigration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
) {
    suspend fun migrateOnce() {
        if (preferences.isSdkCallsMigrated()) return
        WorkManager.getInstance(context).cancelUniqueWork("upload_pending_calls")
        WorkManager.getInstance(context).cancelUniqueWork("upload_pending_calls_kickoff")
        WorkManager.getInstance(context).cancelUniqueWork("reschedule_background_work")
        preferences.markSdkCallsMigrated()
    }
}
