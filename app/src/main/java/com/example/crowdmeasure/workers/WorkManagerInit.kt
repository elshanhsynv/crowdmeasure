package com.example.crowdmeasure.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

object WorkManagerInit {
    @Volatile private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            val appContext = context.applicationContext
            val factory = EntryPointAccessors.fromApplication(
                appContext,
                FactoryEntryPoint::class.java
            ).hiltWorkerFactory()

            val config = Configuration.Builder()
                .setWorkerFactory(factory)
                .setMinimumLoggingLevel(Log.WARN)
                .build()

            WorkManager.initialize(appContext, config)
            initialized = true
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface FactoryEntryPoint {
        fun hiltWorkerFactory(): HiltWorkerFactory
    }
}
