package com.crowdmeasure.sdk

import android.net.Uri
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.DataUsageInfo
import com.crowdmeasure.sdk.model.Location
import com.crowdmeasure.sdk.model.Measurement
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

data class CrowdMeasureConfig(
    val databaseName: String = "crowdmeasure_sdk.db",
    val preferencesName: String = "crowdmeasure_sdk_preferences",
    val defaultEndpointUrl: String = "https://www.google.com/",
    val defaultRetentionDays: Int = 7,
    val collectors: CollectorConfig = CollectorConfig(),
    val publicIpPolicy: PublicIpPolicy = PublicIpPolicy.RAW,
    val performanceProbe: PerformanceProbeConfig = PerformanceProbeConfig(),
    val logger: CrowdMeasureLogger = CrowdMeasureLogger.NONE,
    val requiredDefaultDataMnoId: String? = null,
)

data class CollectorConfig(
    val locationEnabled: Boolean = true,
    val wifiEnabled: Boolean = true,
    val cellularEnabled: Boolean = true,
    val publicIpEnabled: Boolean = true,
    val performanceEnabled: Boolean = true,
)

data class PerformanceProbeConfig(
    val attempts: Int = 8,
    val timeoutMs: Long = 10_000,
) {
    init {
        require(attempts in 1..50) { "attempts must be between 1 and 50" }
        require(timeoutMs in 1_000..120_000) { "timeoutMs must be between 1 and 120000" }
    }
}

enum class PublicIpPolicy { RAW, DISABLED }

fun interface CrowdMeasureLogger {
    fun log(level: Level, message: String, error: Throwable?)

    enum class Level { DEBUG, INFO, WARN, ERROR }

    companion object {
        val NONE = CrowdMeasureLogger { _, _, _ -> }
    }
}

sealed interface SdkResult<out T, out E> {
    data class Success<T>(val value: T) : SdkResult<T, Nothing>
    data class Failure<E>(val error: E) : SdkResult<Nothing, E>
}

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
    data class DefaultDataMnoNotEligible(
        val eligibility: DefaultDataMnoEligibility,
    ) : CrowdMeasureError
}

@Serializable
enum class DefaultDataMnoEligibilityState {
    UNRESTRICTED,
    MATCHED,
    MISMATCHED,
    UNAVAILABLE,
}

@Serializable
data class DefaultDataMnoEligibility(
    val state: DefaultDataMnoEligibilityState = DefaultDataMnoEligibilityState.UNRESTRICTED,
    val requiredMnoId: String? = null,
    val defaultDataMnoId: String? = null,
) {
    val allowsCollection: Boolean
        get() = state == DefaultDataMnoEligibilityState.UNRESTRICTED ||
                state == DefaultDataMnoEligibilityState.MATCHED
}

data class MeasurementRequirements(
    val supportedAndroidVersion: Boolean,
    val locationServicesEnabled: Boolean,
    val missingPermissions: Set<String>,
    val defaultDataMnoEligibility: DefaultDataMnoEligibility = DefaultDataMnoEligibility(),
) {
    // Location can be disabled and I believe for must users it is like that.
    // We can get WIFI stats without location.
    // That means it should not be mandatory.
    // LOCATION - ALL THE TIME OPEN - OH MY GOD!!!
    val canRun: Boolean
        get() = supportedAndroidVersion &&
//                locationServicesEnabled &&
                missingPermissions.isEmpty() && defaultDataMnoEligibility.allowsCollection
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
    fun evaluateDefaultDataMno(): DefaultDataMnoEligibility
}

fun interface CellularSnapshotClient {
    suspend fun collect(): CellInfo
}

fun interface LocationSnapshotClient {
    suspend fun collect(): Location?
}

fun interface DataUsageSnapshotClient {
    suspend fun collect(): DataUsageInfo?
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
