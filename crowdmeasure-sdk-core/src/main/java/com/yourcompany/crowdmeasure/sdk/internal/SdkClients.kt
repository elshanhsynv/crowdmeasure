package com.yourcompany.crowdmeasure.sdk.internal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureError
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureResult
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSettings
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSettingsStore
import com.yourcompany.crowdmeasure.sdk.DataClient
import com.yourcompany.crowdmeasure.sdk.MeasurementClient
import com.yourcompany.crowdmeasure.sdk.MeasurementRequirements
import com.yourcompany.crowdmeasure.sdk.MeasurementQueueClient
import com.yourcompany.crowdmeasure.sdk.MeasurementQueueStatus
import com.yourcompany.crowdmeasure.sdk.MeasurementStore
import com.yourcompany.crowdmeasure.sdk.RequirementsClient
import com.yourcompany.crowdmeasure.sdk.SettingsClient
import com.yourcompany.crowdmeasure.sdk.internal.measurement.MeasurementRunner
import com.yourcompany.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import java.net.URI
import java.util.concurrent.TimeUnit

internal class SdkMeasurementClient(
    context: Context,
    private val settingsStore: CrowdMeasureSettingsStore,
    private val measurementStore: MeasurementStore,
    private val requirementsClient: RequirementsClient,
) : MeasurementClient {
    private val runner = MeasurementRunner(context, settingsStore, Dispatchers.IO)

    override suspend fun runAndSave(): CrowdMeasureResult<Measurement> {
        val requirements = requirementsClient.evaluateManualMeasurement()
        if (!requirements.supportedAndroidVersion) {
            return CrowdMeasureResult.Failure(CrowdMeasureError.UnsupportedAndroidVersion)
        }

        val measurement = runner.runOnce().getOrElse {
            return CrowdMeasureResult.Failure(CrowdMeasureError.CollectionFailed(it))
        }
        return try {
            measurementStore.save(measurement)
            CrowdMeasureResult.Success(measurement)
        } catch (error: Exception) {
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
) : RequirementsClient {
    override fun evaluateManualMeasurement(): MeasurementRequirements {
        val missing = buildSet {
            if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
        }
        val locationManager = context.getSystemService(LocationManager::class.java)
        return MeasurementRequirements(
            supportedAndroidVersion = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
            locationServicesEnabled = runCatching {
                locationManager?.isLocationEnabled == true
            }.getOrDefault(false),
            missingPermissions = missing,
        )
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

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
