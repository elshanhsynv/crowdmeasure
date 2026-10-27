package com.example.crowdmeasure

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.crowdmeasure.domain.usecase.ApplyCollectionSettingsUseCase
import com.example.crowdmeasure.workers.AppBackgroundMigration
import com.example.crowdmeasure.workers.AppUploadMigration
import com.example.crowdmeasure.workers.AppCallsMigration
import com.example.crowdmeasure.workers.ContinuousSessionTrigger
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
    lateinit var applyCollectionSettings: ApplyCollectionSettingsUseCase
    @Inject
    lateinit var continuousSessionTrigger: ContinuousSessionTrigger
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
            // Starts the enabled features (and update checks) only if the current consent is
            // accepted; stops them otherwise.
            applyCollectionSettings()
        }
        // Lives for the whole process so armed sessions start when the next measurement finishes.
        applicationScope.launch { continuousSessionTrigger.run() }
    }
}
