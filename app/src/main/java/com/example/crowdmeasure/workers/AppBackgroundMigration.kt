package com.example.crowdmeasure.workers

import android.content.Context
import androidx.work.WorkManager
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.yourcompany.crowdmeasure.sdk.background.BackgroundResult
import com.yourcompany.crowdmeasure.sdk.background.CrowdMeasureBackground
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBackgroundMigration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
    private val background: BackgroundCollectionClient,
) {
    suspend fun migrateOnce() {
        if (preferences.isSdkBackgroundMigrated()) return
        WorkManager.getInstance(context).cancelUniqueWork("auto_run_measurement")
        WorkManager.getInstance(context).cancelUniqueWork("auto_run_measurement_kickoff")
        WorkManager.getInstance(context).cancelUniqueWork("auto_run_measurement_debug_once")
        WorkManager.getInstance(context).cancelUniqueWork("maintenance_cleanup")
        val settings = preferences.settingsFirst()
        val result = if (settings.autoRunEnabled) {
            background.enable(
                settings.autoRunIntervalMinutes.toLong().coerceIn(
                    CrowdMeasureBackground.MIN_INTERVAL_MINUTES,
                    CrowdMeasureBackground.MAX_INTERVAL_MINUTES,
                ),
                settings.collectOnlyWifi,
            )
        } else {
            background.disable()
        }
        if (result is BackgroundResult.Success) {
            preferences.markSdkBackgroundMigrated()
        }
    }
}
