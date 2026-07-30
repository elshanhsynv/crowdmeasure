package com.crowdmeasure.sdk

import android.content.Context
import com.crowdmeasure.sdk.internal.DefaultCrowdMeasureSettingsStore
import com.crowdmeasure.sdk.internal.DefaultMeasurementStore
import com.crowdmeasure.sdk.internal.SdkDataClient
import com.crowdmeasure.sdk.internal.SdkMeasurementClient
import com.crowdmeasure.sdk.internal.SdkRequirementsClient
import com.crowdmeasure.sdk.internal.SdkMeasurementQueueClient
import com.crowdmeasure.sdk.internal.SdkSettingsClient
import com.crowdmeasure.sdk.internal.measurement.collectors.DataUsageCollector
import com.crowdmeasure.sdk.internal.measurement.collectors.LocationCollector
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
    val location: LocationSnapshotClient,
    val dataUsage: DataUsageSnapshotClient,
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
            val requirements = SdkRequirementsClient(appContext, config.collectors)

            return CrowdMeasureSdk(
                measurements = SdkMeasurementClient(
                    context = appContext,
                    config = config,
                    settingsStore = resolvedSettingsStore,
                    measurementStore = resolvedMeasurementStore,
                    requirementsClient = requirements,
                ),
                data = SdkDataClient(appContext, resolvedMeasurementStore, resolvedSettingsStore),
                settings = SdkSettingsClient(resolvedSettingsStore),
                requirements = requirements,
                queue = SdkMeasurementQueueClient(resolvedMeasurementStore),
                cellular = {
                    withContext(Dispatchers.IO) { TelephonyCollector.collect(appContext) }
                },
                location = {
                    withContext(Dispatchers.IO) { LocationCollector.tryGetCoarseOneShot(appContext) }
                },
                dataUsage = {
                    withContext(Dispatchers.IO) { DataUsageCollector.collect(scope = "calls") }
                },
            )
        }
    }
}
