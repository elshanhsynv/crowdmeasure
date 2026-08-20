package com.example.crowdmeasure.di

import android.content.Context
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
import com.crowdmeasure.sdk.CrowdMeasureConfig
import com.crowdmeasure.sdk.CrowdMeasureLogger
import com.crowdmeasure.sdk.CrowdMeasureSdk
import com.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.crowdmeasure.sdk.background.CrowdMeasureBackground
import com.crowdmeasure.sdk.firestore.measurements.CrowdMeasureFirestoreMeasurements
import com.crowdmeasure.sdk.firestore.calls.CrowdMeasureFirestoreCalls
import com.crowdmeasure.sdk.upload.CrowdMeasureUploads
import com.crowdmeasure.sdk.upload.InstallationIdProvider
import com.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.crowdmeasure.sdk.calls.CallInstallationIdProvider
import com.crowdmeasure.sdk.calls.CallSamplingClient
import com.crowdmeasure.sdk.calls.CallSamplingConfig
import com.crowdmeasure.sdk.calls.CallStore
import com.crowdmeasure.sdk.calls.CrowdMeasureCalls
import com.crowdmeasure.sdk.calls.upload.CallUploadClient
import com.crowdmeasure.sdk.calls.upload.CallUploadConfig
import com.crowdmeasure.sdk.calls.upload.CrowdMeasureCallUploads
import com.example.crowdmeasure.presentation.util.AppLog
import com.example.crowdmeasure.update.ApkVerifier
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun providePrefs(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides @Singleton
    fun provideApkVerifier(): ApkVerifier = ApkVerifier()

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
            requiredDefaultDataMnoId = "40002",
            logger = CrowdMeasureLogger { level, message, error ->
                when (level) {
                    CrowdMeasureLogger.Level.DEBUG -> AppLog.d("SDK", message)
                    CrowdMeasureLogger.Level.INFO -> AppLog.i("SDK", message)
                    CrowdMeasureLogger.Level.WARN -> AppLog.w("SDK", message, error)
                    CrowdMeasureLogger.Level.ERROR -> AppLog.e("SDK", message, error)
                }
            },
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
        uploader = CrowdMeasureFirestoreMeasurements.create(firestore),
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
        callStore = store,
    )

    @Provides @Singleton
    fun provideCallUploadClient(
        @ApplicationContext context: Context,
        calls: CallSamplingClient,
        prefs: AppPreferences,
        firestore: FirebaseFirestore,
    ): CallUploadClient = CrowdMeasureCallUploads.install(
        context,
        calls,
        CallUploadConfig(
            uploader = CrowdMeasureFirestoreCalls.create(firestore),
            installationIdProvider = CallInstallationIdProvider { prefs.installationId() },
        ),
    )

    @Provides @Singleton
    fun provideExporter(@ApplicationContext context: Context): Exporter =
        Exporter(context)
}
