package com.yourcompany.crowdmeasure.sdk

import android.content.Context
import com.yourcompany.crowdmeasure.sdk.internal.DefaultCrowdMeasureSettingsStore
import com.yourcompany.crowdmeasure.sdk.internal.DefaultMeasurementStore
import com.yourcompany.crowdmeasure.sdk.internal.SdkDataClient
import com.yourcompany.crowdmeasure.sdk.internal.SdkMeasurementClient
import com.yourcompany.crowdmeasure.sdk.internal.SdkRequirementsClient
import com.yourcompany.crowdmeasure.sdk.internal.SdkMeasurementQueueClient
import com.yourcompany.crowdmeasure.sdk.internal.SdkSettingsClient
import com.yourcompany.crowdmeasure.sdk.internal.measurement.collectors.TelephonyCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class CrowdMeasureSdk private constructor(
    val measurements: MeasurementClient,
    val data: DataClient,
    val settings: SettingsClient,
    val requirements: RequirementsClient,
    val queue: MeasurementQueueClient,
    val cellular: CellularSnapshotClient,
) {
    companion object {
        fun create(
            context: Context,
            config: CrowdMeasureConfig = CrowdMeasureConfig(),
            measurementStore: MeasurementStore? = null,
            settingsStore: CrowdMeasureSettingsStore? = null,
        ): CrowdMeasureSdk {
            require(config.databaseName.isNotBlank()) { "databaseName must not be blank" }
            require(config.preferencesName.isNotBlank()) { "preferencesName must not be blank" }
            require(config.defaultEndpointUrl.startsWith("https://")) {
                "defaultEndpointUrl must use HTTPS"
            }
            require(config.defaultRetentionDays in 1..90) {
                "defaultRetentionDays must be between 1 and 90"
            }
            if (config.loggingEnabled && Timber.treeCount == 0) {
                Timber.plant(Timber.DebugTree())
            }

            val appContext = context.applicationContext
            val resolvedSettingsStore = settingsStore ?: DefaultCrowdMeasureSettingsStore(
                context = appContext,
                preferencesName = config.preferencesName,
                defaultEndpointUrl = config.defaultEndpointUrl,
                defaultRetentionDays = config.defaultRetentionDays,
            )
            val resolvedMeasurementStore = measurementStore ?: DefaultMeasurementStore.create(
                context = appContext,
                databaseName = config.databaseName,
            )
            val requirements = SdkRequirementsClient(appContext)

            return CrowdMeasureSdk(
                measurements = SdkMeasurementClient(
                    context = appContext,
                    settingsStore = resolvedSettingsStore,
                    measurementStore = resolvedMeasurementStore,
                    requirementsClient = requirements,
                ),
                data = SdkDataClient(appContext, resolvedMeasurementStore, resolvedSettingsStore),
                settings = SdkSettingsClient(resolvedSettingsStore),
                requirements = requirements,
                queue = SdkMeasurementQueueClient(resolvedMeasurementStore),
                cellular = CellularSnapshotClient {
                    withContext(Dispatchers.IO) { TelephonyCollector.collect(appContext) }
                },
            )
        }
    }
}
