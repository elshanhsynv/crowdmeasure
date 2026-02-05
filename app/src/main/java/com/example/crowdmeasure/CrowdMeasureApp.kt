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
import javax.inject.Inject

@HiltAndroidApp
class CrowdMeasureApp : Application() {

    @Inject lateinit var workScheduler: WorkScheduler

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager before anything uses it
        val factory = EntryPointAccessors.fromApplication(
            this,
            WorkManagerFactoryEntryPoint::class.java
        ).hiltWorkerFactory()

        val config = Configuration.Builder()
            .setWorkerFactory(factory)
            .setMinimumLoggingLevel(Log.WARN)
            .build()

        WorkManager.initialize(this, config)

        // Now safe to schedule
        workScheduler.enqueueRescheduleWorker()
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkManagerFactoryEntryPoint {
    fun hiltWorkerFactory(): HiltWorkerFactory
}
