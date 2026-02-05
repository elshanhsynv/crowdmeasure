package com.example.crowdmeasure.di

import android.content.Context
import androidx.work.WorkManager
import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import com.example.crowdmeasure.workers.WorkScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideWorkerStatusStore(@ApplicationContext context: Context): WorkerStatusStore =
        WorkerStatusStore(context)
}
