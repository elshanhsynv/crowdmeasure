package com.crowdmeasure.sdk.calls

import android.net.Uri
import com.crowdmeasure.sdk.DefaultDataMnoEligibility
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.DataUsageInfo
import com.crowdmeasure.sdk.model.Location
import com.crowdmeasure.sdk.model.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class CallSamplingConfig(
    val databaseName: String = "crowdmeasure_calls.db",
    val preferencesName: String = "crowdmeasure_sdk_calls",
    val notificationIconResId: Int,
    val notificationChannelName: String = "Call cell sampling",
    val notificationTitle: String = "Measuring signal quality",
    val notificationText: String = "Collecting network stats during this call.",
    val sampleIntervalSeconds: Int = 5,
    val retentionDays: Int = 7,
)

@Serializable
data class CallSamplingSettings(
    val cellularEnabled: Boolean = DEFAULT_CELLULAR_ENABLED,
    val voipEnabled: Boolean = DEFAULT_VOIP_ENABLED,
) {
    companion object {
        const val DEFAULT_CELLULAR_ENABLED = true
        const val DEFAULT_VOIP_ENABLED = true
    }
}

@Serializable
data class CallSamplingRequirements(
    val supportedAndroidVersion: Boolean,
    val phoneStateGranted: Boolean,
    val fineLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val locationServicesEnabled: Boolean,
    val notificationGranted: Boolean,
    val defaultDataMnoEligibility: DefaultDataMnoEligibility = DefaultDataMnoEligibility(),
//    val batteryOptimizationIgnored: Boolean,
) {
    val canStart: Boolean
        get() = supportedAndroidVersion && phoneStateGranted && fineLocationGranted &&
                backgroundLocationGranted && locationServicesEnabled && notificationGranted &&
                defaultDataMnoEligibility.allowsCollection
}

@Serializable
data class MissedCallStart(val atUtcMs: Long, val code: CallRunCode)

@Serializable
data class CallSamplingStatus(
    val settings: CallSamplingSettings,
    val requirements: CallSamplingRequirements,
    val activeSession: CallSession?,
    val voipMonitorActive: Boolean,
    val lastMissedStart: MissedCallStart?,
)

@Serializable
enum class CallType { INCOMING, OUTGOING, UNKNOWN }

@Serializable
enum class CallSource { CELLULAR, WHATSAPP_VOICE, WHATSAPP_VIDEO, WHATSAPP_UNKNOWN, VOIP_GENERIC, UNKNOWN }

@Serializable
enum class CallUploadState { PENDING, UPLOADED, FAILED }

@Serializable
enum class CallRunCode {
    OK, DISABLED, NOT_INSTALLED, MISSING_PHONE_STATE, MISSING_FINE_LOCATION,
    MISSING_BACKGROUND_LOCATION, LOCATION_SERVICES_DISABLED, MISSING_NOTIFICATIONS,
    TARGET_MNO_NOT_DEFAULT, TARGET_MNO_UNAVAILABLE,
    UNSUPPORTED_ANDROID, CALL_NOT_ACTIVE, FOREGROUND_SERVICE_FAILED, FOREGROUND_SERVICE_START_NOT_ALLOWED,
    FOREGROUND_SERVICE_PERMISSION_DENIED, BACKEND_REJECTED, TRANSIENT_FAILURE,
    SERIALIZATION_FAILED, PERSISTENCE_FAILED, INVALID_CONFIGURATION, UNEXPECTED_ERROR
}

@Serializable
data class CallSession(
    val sessionId: String,
    val startedAtUtcMs: Long,
    val endedAtUtcMs: Long?,
    val callType: CallType,
    val callSource: CallSource,
    val sampleIntervalSeconds: Int,
    val sampleCount: Int,
    val endReason: String?,
    val uploadState: CallUploadState = CallUploadState.PENDING,
    val simCarriers: List<CarrierInfo> = emptyList(),
    val latestSample: CallCellSample? = null,
    val transportType: TransportType? = null
)

@Serializable
data class CallCellSample(
    val id: Long,
    val sessionId: String,
    val sampledAtUtcMs: Long,
    val elapsedMs: Long,
    val cell: CellInfo,
    val rat: String?,
    val nrState: String?,
    val dbm: Int?,
    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val pci: Int?,
    val tac: Int?,
    val band: Int?,
    val location: Location? = null,
    val dataUsage: DataUsageInfo? = null,
    val transportType: TransportType? = null,
)

@Serializable
data class CallSessionExport(val session: CallSession, val samples: List<CallCellSample>)

@Serializable
data class CallUploadItem(
    val session: CallSession,
    val samples: List<CallCellSample>,
    val installId: String,
    val deviceModel: String
)

@Serializable
data class CallUploadBatchResult(
    val uploadedSessionIds: Set<String> = emptySet(),
    val retryableSessionIds: Set<String> = emptySet(),
    val rejectedSessionIds: Set<String> = emptySet(),
)

sealed interface CallUploaderResult {
    data class Success(val result: CallUploadBatchResult) : CallUploaderResult
    data class Failure(val error: CallSamplingError) : CallUploaderResult
}

fun interface CallUploader {
    suspend fun upload(items: List<CallUploadItem>): CallUploaderResult
}

fun interface CallInstallationIdProvider {
    suspend fun getInstallationId(): String
}

sealed interface CallSamplingResult<out T> {
    data class Success<T>(val value: T) : CallSamplingResult<T>
    data class Failure(val error: CallSamplingError) : CallSamplingResult<Nothing>
}

sealed interface CallSamplingError {
    data object Disabled : CallSamplingError
    data object NotInstalled : CallSamplingError
    data class MissingRequirements(val requirements: CallSamplingRequirements) : CallSamplingError
    data class DefaultDataMnoNotEligible(
        val eligibility: DefaultDataMnoEligibility,
    ) : CallSamplingError
    data class InvalidConfiguration(val message: String) : CallSamplingError
    data class BackendRejected(val cause: Throwable? = null) : CallSamplingError
    data class TransientFailure(val cause: Throwable? = null) : CallSamplingError
    data class SerializationFailure(val cause: Throwable? = null) : CallSamplingError
    data class PersistenceFailure(val cause: Throwable? = null) : CallSamplingError
    data class SchedulingFailure(val cause: Throwable) : CallSamplingError
    data class ExportFailure(val cause: Throwable) : CallSamplingError
}

interface CallStore {
    suspend fun getActiveSession(): CallSession?
    suspend fun startSession(
        callType: CallType,
        callSource: CallSource,
        intervalSeconds: Int,
        transportType: TransportType? = null
    ): CallSession

    suspend fun insertSample(
        sessionId: String,
        sampledAtUtcMs: Long,
        elapsedMs: Long,
        cellInfo: CellInfo,
        location: Location? = null,
        dataUsage: DataUsageInfo? = null,
        transportType: TransportType? = null
    )

    suspend fun finishSession(sessionId: String, endedAtUtcMs: Long, endReason: String)
    suspend fun finishActiveSession(endedAtUtcMs: Long, endReason: String)
    suspend fun reclassifySession(sessionId: String, callType: CallType, callSource: CallSource)
    fun observeSessions(limit: Int): Flow<List<CallSession>>
    fun observeSamples(sessionId: String): Flow<List<CallCellSample>>
    suspend fun getRecentSessions(limit: Int): List<CallSessionExport>
    suspend fun getUploadCandidates(limit: Int): List<CallSessionExport>
    suspend fun markUploaded(sessionIds: List<String>)
    suspend fun markFailed(sessionIds: List<String>)
    suspend fun deleteOlderThan(cutoffUtcMs: Long)
    suspend fun deleteAll()
}

interface CallSamplingClient {
    suspend fun setCellularSamplingEnabled(enabled: Boolean): CallSamplingResult<Unit>
    suspend fun setVoipSamplingEnabled(enabled: Boolean): CallSamplingResult<Unit>
    suspend fun activateEnabledFeatures(): CallSamplingResult<Unit>
    fun observeSettings(): Flow<CallSamplingSettings>
    fun observeRequirements(): Flow<CallSamplingRequirements>
    fun observeStatus(): Flow<CallSamplingStatus>
    fun observeSessions(limit: Int = 50): Flow<List<CallSession>>
    fun observeSamples(sessionId: String): Flow<List<CallCellSample>>
    suspend fun exportSessions(lastN: Int = 100): CallSamplingResult<Uri>
    suspend fun deleteAll(): CallSamplingResult<Unit>
    val uploadQueue: CallUploadQueueClient
}

interface CallUploadQueueClient {
    suspend fun getCandidates(limit: Int = 50): List<CallSessionExport>
    suspend fun markUploaded(sessionIds: List<String>): CallSamplingResult<Unit>
    suspend fun markFailed(sessionIds: List<String>): CallSamplingResult<Unit>
}
