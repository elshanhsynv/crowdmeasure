package com.example.crowdmeasure

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.crowdmeasure.workers.AppBackgroundMigration
import com.example.crowdmeasure.workers.AppUploadMigration
import com.example.crowdmeasure.workers.AppCallsMigration
import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.upload.CallUploadClient
import com.example.crowdmeasure.update.UpdateScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class CrowdMeasureApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var appBackgroundMigration: AppBackgroundMigration
    @Inject
    lateinit var appUploadMigration: AppUploadMigration
    @Inject
    lateinit var appCallsMigration: AppCallsMigration
    @Inject
    lateinit var calls: CallSamplingClient
    @Inject
    lateinit var callUploads: CallUploadClient
    @Inject
    lateinit var updateScheduler: UpdateScheduler
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.WARN)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        applicationScope.launch {
            appBackgroundMigration.migrateOnce()
            appUploadMigration.migrateOnce()
            appCallsMigration.migrateOnce()
            calls.activateEnabledFeatures()
            callUploads.reschedule()
            updateScheduler.schedulePeriodicChecks()
        }
    }
}
