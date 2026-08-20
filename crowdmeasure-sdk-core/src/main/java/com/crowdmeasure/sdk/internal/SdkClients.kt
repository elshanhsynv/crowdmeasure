package com.crowdmeasure.sdk.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.crowdmeasure.sdk.CollectorConfig
import com.crowdmeasure.sdk.CrowdMeasureError
import com.crowdmeasure.sdk.CrowdMeasureLogger
import com.crowdmeasure.sdk.CrowdMeasureResult
import com.crowdmeasure.sdk.CrowdMeasureSettings
import com.crowdmeasure.sdk.CrowdMeasureSettingsStore
import com.crowdmeasure.sdk.CrowdMeasureConfig
import com.crowdmeasure.sdk.DataClient
import com.crowdmeasure.sdk.MeasurementClient
import com.crowdmeasure.sdk.MeasurementRequirements
import com.crowdmeasure.sdk.MeasurementQueueClient
import com.crowdmeasure.sdk.MeasurementQueueStatus
import com.crowdmeasure.sdk.MeasurementStore
import com.crowdmeasure.sdk.RequirementsClient
import com.crowdmeasure.sdk.SettingsClient
import com.crowdmeasure.sdk.internal.measurement.MeasurementRunner
import com.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

internal class SdkMeasurementClient(
    context: Context,
    config: CrowdMeasureConfig,
    settingsStore: CrowdMeasureSettingsStore,
    private val measurementStore: MeasurementStore,
    private val requirementsClient: RequirementsClient,
) : MeasurementClient {
    private val logger = config.logger
    private val runner = MeasurementRunner(
        context,
        settingsStore,
        config,
        Dispatchers.IO,
        logger,
    )

    override suspend fun runAndSave(): CrowdMeasureResult<Measurement> {
        logger.info("Manual measurement requested.")
        val requirements = requirementsClient.evaluateManualMeasurement()
        logger.info("Measurement requirements: ${requirements.describe()}")
        if (!requirements.supportedAndroidVersion) {
            logger.warn("Measurement blocked: unsupported Android version.")
            return CrowdMeasureResult.Failure(CrowdMeasureError.UnsupportedAndroidVersion)
        }
        if (requirements.missingPermissions.isNotEmpty()) {
            logger.warn(
                "Measurement blocked: missing permissions " +
                        requirements.missingPermissions.joinToString(),
            )
            return CrowdMeasureResult.Failure(
                CrowdMeasureError.MissingPermissions(requirements.missingPermissions),
            )
        }
        if (!requirements.defaultDataMnoEligibility.allowsCollection) {
            logger.warn("Measurement blocked: default data MNO is not eligible.")
            return CrowdMeasureResult.Failure(
                CrowdMeasureError.DefaultDataMnoNotEligible(requirements.defaultDataMnoEligibility),
            )
        }
//        if (!requirements.locationServicesEnabled) {
//            logger.warn("Measurement blocked: location services are disabled.")
//            return CrowdMeasureResult.Failure(CrowdMeasureError.LocationServicesDisabled)
//        }

        val measurement = runner.runOnce().getOrElse {
            if (it is CancellationException) throw it
            logger.error("Measurement collection failed.", it)
            return CrowdMeasureResult.Failure(CrowdMeasureError.CollectionFailed(it))
        }
        return try {
            measurementStore.save(measurement)
            logger.info("Measurement saved: id=${measurement.meta.measurementId}.")
            CrowdMeasureResult.Success(measurement)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            logger.error("Measurement persistence failed.", error)
            CrowdMeasureResult.Failure(CrowdMeasureError.PersistenceFailed(error))
        }
    }

    override fun observeLatest(): Flow<Measurement?> = measurementStore.observeLatest()

    override fun observeHistory(limit: Int): Flow<List<Measurement>> =
        measurementStore.observeHistory(limit.coerceIn(1, 10_000))

    override suspend fun getById(id: String): Measurement? = measurementStore.getById(id)
}

internal class SdkDataClient(
    context: Context,
    private val measurementStore: MeasurementStore,
    private val settingsStore: CrowdMeasureSettingsStore,
) : DataClient {
    private val exporter = MeasurementExporter(context)

    override suspend fun exportMeasurements(lastN: Int): CrowdMeasureResult<Uri> =
        exporter.export(measurementStore.getLastN(lastN.coerceIn(1, 10_000)))

    override suspend fun deleteAllMeasurements(): CrowdMeasureResult<Unit> =
        try {
            measurementStore.deleteAll()
            CrowdMeasureResult.Success(Unit)
        } catch (error: Exception) {
            CrowdMeasureResult.Failure(CrowdMeasureError.PersistenceFailed(error))
        }

    override suspend fun pruneExpiredMeasurements(nowUtcMs: Long): CrowdMeasureResult<Int> =
        try {
            val retentionDays = settingsStore.settings.first().retentionDays
            val cutoffUtcMs = nowUtcMs - TimeUnit.DAYS.toMillis(retentionDays.toLong())
            CrowdMeasureResult.Success(measurementStore.deleteOlderThan(cutoffUtcMs))
        } catch (error: Exception) {
            CrowdMeasureResult.Failure(CrowdMeasureError.PersistenceFailed(error))
        }
}

internal class SdkSettingsClient(
    private val store: CrowdMeasureSettingsStore,
) : SettingsClient {
    override fun observeSettings(): Flow<CrowdMeasureSettings> = store.settings

    override suspend fun setEndpointUrl(url: String): CrowdMeasureResult<Unit> {
        val valid = runCatching {
            val uri = URI(url.trim())
            uri.scheme == "https" && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
        if (!valid) {
            return CrowdMeasureResult.Failure(
                CrowdMeasureError.InvalidConfiguration("Endpoint must be a valid HTTPS URL")
            )
        }
        return runCatching { store.setEndpointUrl(url.trim()) }
            .fold(
                onSuccess = { CrowdMeasureResult.Success(Unit) },
                onFailure = { CrowdMeasureResult.Failure(CrowdMeasureError.PersistenceFailed(it)) },
            )
    }

    override suspend fun setRetentionDays(days: Int): CrowdMeasureResult<Unit> {
        if (days !in 1..90) {
            return CrowdMeasureResult.Failure(
                CrowdMeasureError.InvalidConfiguration("Retention days must be between 1 and 90")
            )
        }
        return runCatching { store.setRetentionDays(days) }
            .fold(
                onSuccess = { CrowdMeasureResult.Success(Unit) },
                onFailure = { CrowdMeasureResult.Failure(CrowdMeasureError.PersistenceFailed(it)) },
            )
    }
}

internal class SdkRequirementsClient(
    private val context: Context,
    private val collectors: CollectorConfig,
    private val mnoEligibilityEvaluator: DefaultDataMnoEligibilityEvaluator,
) : RequirementsClient {
    override fun evaluateManualMeasurement(): MeasurementRequirements {
        val missing = buildSet {
            val needsLocationPermission = collectors.locationEnabled ||
                    collectors.wifiEnabled ||
                    collectors.cellularEnabled
            if (needsLocationPermission && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (collectors.cellularEnabled && !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if ((collectors.cellularEnabled || mnoEligibilityEvaluator.isRestricted()) &&
                !hasPermission(Manifest.permission.READ_PHONE_STATE)
            ) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
        }
        val locationManager = context.getSystemService(LocationManager::class.java)
        val needsLocationServices = collectors.locationEnabled ||
                collectors.wifiEnabled ||
                collectors.cellularEnabled
        return MeasurementRequirements(
            supportedAndroidVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            locationServicesEnabled = !needsLocationServices || runCatching {
                locationManager?.isLocationEnabled == true
            }.getOrDefault(false),
            missingPermissions = missing,
            defaultDataMnoEligibility = evaluateDefaultDataMno(),
        )
    }

    override fun evaluateDefaultDataMno() = mnoEligibilityEvaluator.evaluate()

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun CrowdMeasureLogger.debug(message: String) =
    log(CrowdMeasureLogger.Level.DEBUG, message, null)

private fun CrowdMeasureLogger.info(message: String) =
    log(CrowdMeasureLogger.Level.INFO, message, null)

private fun CrowdMeasureLogger.warn(message: String) =
    log(CrowdMeasureLogger.Level.WARN, message, null)

private fun CrowdMeasureLogger.error(message: String, error: Throwable) =
    log(CrowdMeasureLogger.Level.ERROR, message, error)

private fun MeasurementRequirements.describe(): String =
    "supportedAndroidVersion=$supportedAndroidVersion, " +
            "locationServicesEnabled=$locationServicesEnabled, " +
            "missingPermissions=${missingPermissions.joinToString().ifBlank { "none" }}, " +
            "defaultDataMno=${defaultDataMnoEligibility.state}"

internal class SdkMeasurementQueueClient(
    private val store: MeasurementStore,
) : MeasurementQueueClient {
    override fun observeStatus(): Flow<MeasurementQueueStatus> =
        combine(store.observePendingCount(), store.observeFailedCount()) { pending, failed ->
            MeasurementQueueStatus(pending, failed)
        }

    override suspend fun getCandidates(limit: Int): List<Measurement> =
        store.getUploadCandidates(limit.coerceIn(1, 1_000))

    override suspend fun markUploaded(ids: List<String>): CrowdMeasureResult<Unit> =
        update(ids, store::markUploaded)

    override suspend fun markFailed(ids: List<String>): CrowdMeasureResult<Unit> =
        update(ids, store::markFailed)

    private suspend fun update(
        ids: List<String>,
        action: suspend (List<String>) -> Unit,
    ): CrowdMeasureResult<Unit> = try {
        action(ids.distinct())
        CrowdMeasureResult.Success(Unit)
    } catch (error: Exception) {
        CrowdMeasureResult.Failure(CrowdMeasureError.PersistenceFailed(error))
    }
}
