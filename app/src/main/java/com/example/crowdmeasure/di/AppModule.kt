package com.example.crowdmeasure.di

import android.content.Context
import com.example.crowdmeasure.data.export.Exporter
import com.example.crowdmeasure.data.measurement.MeasurementRunner
import com.example.crowdmeasure.data.measurement.net.OkHttpClientProvider
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.data.repo.MeasurementRepositoryImpl
import com.example.crowdmeasure.domain.repo.UploadRepositoryFirestore
import com.example.crowdmeasure.data.repo.UserSessionRepositoryImpl
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UploadRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun providePrefs(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides @Singleton
    fun provideSessionRepo(prefs: AppPreferences): UserSessionRepository =
        UserSessionRepositoryImpl(prefs)

    @Provides @Singleton
    fun provideOkHttpProvider(): OkHttpClientProvider =
        OkHttpClientProvider()

    @Provides @Singleton
    fun provideMeasurementRunner(
        @ApplicationContext context: Context,
        prefs: AppPreferences,
        okHttpClientProvider: OkHttpClientProvider,
        @IoDispatcher io: CoroutineDispatcher
    ): MeasurementRunner =
        MeasurementRunner(context, prefs, okHttpClientProvider, io)

    @Provides @Singleton
    fun provideMeasurementRepo(
        dao: com.example.crowdmeasure.data.db.MeasurementDao,
        runner: MeasurementRunner,
        @IoDispatcher io: CoroutineDispatcher
    ): MeasurementRepository =
        MeasurementRepositoryImpl(dao, runner, io)

    @Provides @Singleton
    fun provideUploadRepo(
        dao: com.example.crowdmeasure.data.db.MeasurementDao,
        prefs: AppPreferences,
        firestore: FirebaseFirestore
    ): UploadRepository = UploadRepositoryFirestore(dao, prefs, firestore)

    @Provides @Singleton
    fun provideExporter(@ApplicationContext context: Context): Exporter =
        Exporter(context)
}