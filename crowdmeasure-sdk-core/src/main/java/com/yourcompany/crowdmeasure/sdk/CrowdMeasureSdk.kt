package com.crowdmeasure.sdk

import android.content.Context
import com.crowdmeasure.sdk.internal.DefaultCrowdMeasureSettingsStore
import com.crowdmeasure.sdk.internal.DefaultMeasurementStore
import com.crowdmeasure.sdk.internal.DefaultIpHashSaltProvider
import com.crowdmeasure.sdk.internal.SdkDataClient
import com.crowdmeasure.sdk.internal.SdkMeasurementClient
import com.crowdmeasure.sdk.internal.SdkRequirementsClient
import com.crowdmeasure.sdk.internal.SdkMeasurementQueueClient
import com.crowdmeasure.sdk.internal.SdkSettingsClient
import com.crowdmeasure.sdk.internal.measurement.collectors.TelephonyCollector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            val appContext = context.applicationContext
            val resolvedIpHashSaltProvider = config.ipHashSaltProvider
                ?: DefaultIpHashSaltProvider(appContext, "${config.preferencesName}_privacy")
            val resolvedSettingsStore = settingsStore ?: DefaultCrowdMeasureSettingsStore(
                context = appContext,
                preferencesName = config.preferencesName,
                defaultEndpointUrl = config.defaultEndpointUrl,
                defaultRetentionDays = config.defaultRetentionDays,
            )
            val resolvedMeasurementStore = measurementStore ?: DefaultMeasurementStore.create(
                context = appContext,
                databaseName = config.databaseName,
                ipHashSaltProvider = resolvedIpHashSaltProvider,
            )
            val requirements = SdkRequirementsClient(appContext)

            return CrowdMeasureSdk(
                measurements = SdkMeasurementClient(
                    context = appContext,
                    config = config,
                    ipHashSaltProvider = resolvedIpHashSaltProvider,
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
