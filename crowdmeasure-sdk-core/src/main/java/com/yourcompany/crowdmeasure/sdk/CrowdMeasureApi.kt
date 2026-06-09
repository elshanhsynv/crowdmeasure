package com.yourcompany.crowdmeasure.sdk

import android.net.Uri
import com.yourcompany.crowdmeasure.sdk.model.CellInfo
import com.yourcompany.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.flow.Flow

data class CrowdMeasureConfig(
    val databaseName: String = "crowdmeasure_sdk.db",
    val preferencesName: String = "crowdmeasure_sdk_preferences",
    val defaultEndpointUrl: String = "https://www.google.com/",
    val defaultRetentionDays: Int = 7,
    val loggingEnabled: Boolean = false,
)

data class CrowdMeasureSettings(
    val endpointUrl: String,
    val retentionDays: Int,
)

sealed interface CrowdMeasureResult<out T> {
    data class Success<T>(val value: T) : CrowdMeasureResult<T>
    data class Failure(val error: CrowdMeasureError) : CrowdMeasureResult<Nothing>
}

sealed interface CrowdMeasureError {
    data class MissingPermissions(val permissions: Set<String>) : CrowdMeasureError
    data object UnsupportedAndroidVersion : CrowdMeasureError
    data object LocationServicesDisabled : CrowdMeasureError
    data class CollectionFailed(val cause: Throwable) : CrowdMeasureError
    data class PersistenceFailed(val cause: Throwable) : CrowdMeasureError
    data class ExportFailed(val cause: Throwable) : CrowdMeasureError
    data class InvalidConfiguration(val message: String) : CrowdMeasureError
}

data class MeasurementRequirements(
    val supportedAndroidVersion: Boolean,
    val locationServicesEnabled: Boolean,
    val missingPermissions: Set<String>,
) {
    val canRun: Boolean get() = supportedAndroidVersion
}

interface MeasurementClient {
    suspend fun runAndSave(): CrowdMeasureResult<Measurement>
    fun observeLatest(): Flow<Measurement?>
    fun observeHistory(limit: Int = 100): Flow<List<Measurement>>
    suspend fun getById(id: String): Measurement?
}

interface DataClient {
    suspend fun exportMeasurements(lastN: Int): CrowdMeasureResult<Uri>
    suspend fun deleteAllMeasurements(): CrowdMeasureResult<Unit>
    suspend fun pruneExpiredMeasurements(nowUtcMs: Long = System.currentTimeMillis()): CrowdMeasureResult<Int>
}

interface SettingsClient {
    fun observeSettings(): Flow<CrowdMeasureSettings>
    suspend fun setEndpointUrl(url: String): CrowdMeasureResult<Unit>
    suspend fun setRetentionDays(days: Int): CrowdMeasureResult<Unit>
}

interface RequirementsClient {
    fun evaluateManualMeasurement(): MeasurementRequirements
}

fun interface CellularSnapshotClient {
    suspend fun collect(): CellInfo
}

data class MeasurementQueueStatus(
    val pendingCount: Int,
    val failedCount: Int,
)

interface MeasurementQueueClient {
    fun observeStatus(): Flow<MeasurementQueueStatus>
    suspend fun getCandidates(limit: Int = 50): List<Measurement>
    suspend fun markUploaded(ids: List<String>): CrowdMeasureResult<Unit>
    suspend fun markFailed(ids: List<String>): CrowdMeasureResult<Unit>
}
