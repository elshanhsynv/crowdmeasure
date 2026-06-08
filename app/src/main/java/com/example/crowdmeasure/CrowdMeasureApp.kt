package com.example.crowdmeasure

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.crowdmeasure.workers.WorkScheduler
import com.example.crowdmeasure.callsampling.VoipCallMonitor
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CrowdMeasureApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var workScheduler: WorkScheduler
    @Inject
    lateinit var voipCallMonitor: VoipCallMonitor

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
        voipCallMonitor.start()
    }
}
