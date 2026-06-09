package com.example.crowdmeasure.di

import android.content.Context
import com.example.crowdmeasure.BuildConfig
import com.example.crowdmeasure.data.export.Exporter
import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.data.repo.AppMeasurementStore
import com.example.crowdmeasure.data.repo.AppSdkSettingsStore
import com.example.crowdmeasure.data.repo.CallSamplingRepositoryImpl
import com.example.crowdmeasure.data.repo.MeasurementRepositoryImpl
import com.example.crowdmeasure.data.repo.UserSessionRepositoryImpl
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureConfig
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.yourcompany.crowdmeasure.sdk.background.CrowdMeasureBackground
import com.yourcompany.crowdmeasure.sdk.firestore.CrowdMeasureFirestore
import com.yourcompany.crowdmeasure.sdk.upload.CrowdMeasureUploads
import com.yourcompany.crowdmeasure.sdk.upload.InstallationIdProvider
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.yourcompany.crowdmeasure.sdk.calls.CallInstallationIdProvider
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingClient
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingConfig
import com.yourcompany.crowdmeasure.sdk.calls.CallStore
import com.yourcompany.crowdmeasure.sdk.calls.CrowdMeasureCalls
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
    fun provideCrowdMeasureSdk(
        @ApplicationContext context: Context,
        prefs: AppPreferences,
        dao: com.example.crowdmeasure.data.db.MeasurementDao,
    ): CrowdMeasureSdk = CrowdMeasureSdk.create(
        context = context,
        config = CrowdMeasureConfig(
            databaseName = "crowdmeasure.db",
            defaultEndpointUrl = AppPreferences.DEFAULT_ENDPOINT,
            defaultRetentionDays = AppPreferences.DEFAULT_RETENTION_DAYS,
            loggingEnabled = BuildConfig.DEBUG,
        ),
        measurementStore = AppMeasurementStore(dao),
        settingsStore = AppSdkSettingsStore(prefs),
    )

    @Provides @Singleton
    fun provideBackgroundCollectionClient(
        @ApplicationContext context: Context,
        sdk: CrowdMeasureSdk,
    ): BackgroundCollectionClient = CrowdMeasureBackground.install(context, sdk)

    @Provides @Singleton
    fun provideMeasurementUploadClient(
        @ApplicationContext context: Context,
        sdk: CrowdMeasureSdk,
        prefs: AppPreferences,
        firestore: FirebaseFirestore,
    ): MeasurementUploadClient = CrowdMeasureUploads.install(
        context = context,
        sdk = sdk,
        uploader = CrowdMeasureFirestore.create(firestore),
        installationIdProvider = InstallationIdProvider { prefs.installationId() },
    )

    @Provides @Singleton
    fun provideMeasurementRepo(
        dao: com.example.crowdmeasure.data.db.MeasurementDao,
        sdk: CrowdMeasureSdk,
        @IoDispatcher io: CoroutineDispatcher
    ): MeasurementRepository =
        MeasurementRepositoryImpl(dao, sdk, io)

    @Provides @Singleton
    fun provideCallSamplingRepositoryImpl(
        dao: com.example.crowdmeasure.data.db.CallSamplingDao,
        @IoDispatcher io: CoroutineDispatcher
    ): CallSamplingRepositoryImpl = CallSamplingRepositoryImpl(dao, io)

    @Provides
    fun provideCallStore(impl: CallSamplingRepositoryImpl): CallStore = impl

    @Provides @Singleton
    fun provideCallSamplingClient(
        @ApplicationContext context: Context,
        sdk: CrowdMeasureSdk,
        store: CallStore,
        prefs: AppPreferences,
        firestore: FirebaseFirestore
    ): CallSamplingClient = CrowdMeasureCalls.install(
        context = context,
        sdk = sdk,
        config = CallSamplingConfig(notificationIconResId = com.example.crowdmeasure.R.drawable.crowdmeasure),
        uploader = CrowdMeasureFirestore.createCallUploader(firestore),
        callStore = store,
        installationIdProvider = CallInstallationIdProvider { prefs.installationId() },
    )

    @Provides @Singleton
    fun provideExporter(@ApplicationContext context: Context): Exporter =
        Exporter(context)
}
