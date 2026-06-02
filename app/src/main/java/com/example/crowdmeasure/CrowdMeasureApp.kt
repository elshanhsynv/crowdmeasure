package com.example.crowdmeasure

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.crowdmeasure.workers.WorkScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CrowdMeasureApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workScheduler: WorkScheduler

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

        workScheduler.enqueueRescheduleWorker()
    }
}

//@EntryPoint
//@InstallIn(SingletonComponent::class)
//interface WorkManagerFactoryEntryPoint {
//    fun hiltWorkerFactory(): HiltWorkerFactory
//}
