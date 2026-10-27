package com.example.crowdmeasure.di

import android.content.Context
import com.example.crowdmeasure.data.prefs.ContinuousSessionPrefs
import com.example.crowdmeasure.data.repo.ContinuousSessionRepositoryImpl
import com.example.crowdmeasure.domain.repo.ContinuousSessionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContinuousSessionModule {

    @Provides
    @Singleton
    fun provideContinuousSessionPrefs(@ApplicationContext context: Context): ContinuousSessionPrefs =
        ContinuousSessionPrefs(context)

    @Provides
    @Singleton
    fun provideContinuousSessionRepo(
        @ApplicationContext context: Context,
        prefs: ContinuousSessionPrefs,
    ): ContinuousSessionRepository = ContinuousSessionRepositoryImpl(context, prefs)
}
