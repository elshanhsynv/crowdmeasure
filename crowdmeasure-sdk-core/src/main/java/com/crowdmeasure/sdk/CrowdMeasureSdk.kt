package com.crowdmeasure.sdk

import android.content.Context
import android.content.pm.ApplicationInfo
import com.crowdmeasure.sdk.internal.isValidHttpsEndpoint
import com.crowdmeasure.sdk.internal.DefaultCrowdMeasureSettingsStore
import com.crowdmeasure.sdk.internal.DefaultDataMnoEligibilityEvaluator
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
            config: CrowdMeasureConfig,
            measurementStore: MeasurementStore? = null,
            settingsStore: CrowdMeasureSettingsStore? = null,
        ): CrowdMeasureSdk {
            require(config.databaseName.isNotBlank()) { "databaseName must not be blank" }
            require(config.preferencesName.isNotBlank()) { "preferencesName must not be blank" }
            require(isValidHttpsEndpoint(config.defaultEndpointUrl)) {
                "defaultEndpointUrl must be a valid HTTPS URL without credentials"
            }
            require(config.defaultRetentionDays in 1..90) {
                "defaultRetentionDays must be between 1 and 90"
            }
            require(config.requiredDefaultDataMnoId == null ||
                    config.requiredDefaultDataMnoId.trim().isNotEmpty()) {
                "requiredDefaultDataMnoId must not be blank"
            }
            val appContext = context.applicationContext
            warnIfBackupAllowed(appContext, config.logger)
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
            val requirements = SdkRequirementsClient(
                context = appContext,
                collectors = config.collectors,
                mnoEligibilityEvaluator = DefaultDataMnoEligibilityEvaluator(
                    appContext,
                    config.requiredDefaultDataMnoId,
                    config.requireNotRoaming,
                ),
            )

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

        /**
         * A library cannot override the host's backup rules, and the SDK databases hold location
         * and call history. The SDK cannot read the host's exclusion rules, so this warns whenever
         * backup is on; hosts that already exclude the SDK files can ignore it.
         */
        private fun warnIfBackupAllowed(context: Context, logger: CrowdMeasureLogger) {
            if ((context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) == 0) return
            logger.log(
                CrowdMeasureLogger.Level.WARN,
                "android:allowBackup is enabled. Exclude the CrowdMeasure databases and " +
                    "preferences from backup and device transfer; see docs/data-storage.md.",
                null,
            )
        }
    }
}
