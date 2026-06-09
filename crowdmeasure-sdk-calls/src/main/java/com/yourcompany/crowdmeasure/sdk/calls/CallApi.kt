package com.yourcompany.crowdmeasure.sdk.calls

import android.net.Uri
import com.yourcompany.crowdmeasure.sdk.model.CellInfo
import kotlinx.coroutines.flow.Flow

data class CallSamplingConfig(
    val databaseName: String = "crowdmeasure_calls.db",
    val preferencesName: String = "crowdmeasure_calls_preferences",
    val notificationIconResId: Int,
    val notificationChannelName: String = "Call cell sampling",
    val notificationTitle: String = "Measuring signal quality",
    val notificationText: String = "Collecting network stats during this call.",
    val sampleIntervalSeconds: Int = 5,
    val retentionDays: Int = 7,
)

data class CallSamplingSettings(
    val cellularEnabled: Boolean = false,
    val voipEnabled: Boolean = false,
    val uploadsEnabled: Boolean = false,
    val uploadIntervalMinutes: Long = 60,
    val uploadWifiOnly: Boolean = true,
)

data class CallSamplingRequirements(
    val supportedAndroidVersion: Boolean,
    val phoneStateGranted: Boolean,
    val fineLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val locationServicesEnabled: Boolean,
    val notificationGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
) {
    val canStart: Boolean get() = supportedAndroidVersion && phoneStateGranted && fineLocationGranted &&
        backgroundLocationGranted && locationServicesEnabled && notificationGranted
}

data class MissedCallStart(val atUtcMs: Long, val code: CallRunCode)
data class CallSamplingStatus(
    val settings: CallSamplingSettings,
    val requirements: CallSamplingRequirements,
    val activeSession: CallSession?,
    val voipMonitorActive: Boolean,
    val lastMissedStart: MissedCallStart?,
    val uploadWorkState: CallUploadWorkState,
)

enum class CallType { INCOMING, OUTGOING, UNKNOWN }
enum class CallSource { CELLULAR, WHATSAPP_VOICE, WHATSAPP_VIDEO, WHATSAPP_UNKNOWN, VOIP_GENERIC, UNKNOWN }
enum class CallUploadState { PENDING, UPLOADED, FAILED }
enum class CallUploadWorkState { DISABLED, ENQUEUED, RUNNING, BLOCKED, SUCCEEDED, FAILED, CANCELLED, UNKNOWN }
enum class CallRunCode {
    OK, DISABLED, NOT_INSTALLED, MISSING_PHONE_STATE, MISSING_FINE_LOCATION,
    MISSING_BACKGROUND_LOCATION, LOCATION_SERVICES_DISABLED, MISSING_NOTIFICATIONS,
    UNSUPPORTED_ANDROID, FOREGROUND_SERVICE_FAILED, BACKEND_REJECTED, TRANSIENT_FAILURE,
    SERIALIZATION_FAILED, PERSISTENCE_FAILED, INVALID_CONFIGURATION, UNEXPECTED_ERROR
}

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
    val latestSample: CallCellSample? = null,
)

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
)

data class CallSessionExport(val session: CallSession, val samples: List<CallCellSample>)
data class CallUploadItem(val session: CallSession, val samples: List<CallCellSample>, val installId: String, val deviceModel: String)
data class CallUploadBatchResult(val uploadedSessionIds: List<String>)

sealed interface CallUploaderResult {
    data class Success(val result: CallUploadBatchResult) : CallUploaderResult
    data class Failure(val error: CallSamplingError) : CallUploaderResult
}
fun interface CallUploader { suspend fun upload(items: List<CallUploadItem>): CallUploaderResult }
fun interface CallInstallationIdProvider { suspend fun getInstallationId(): String }

sealed interface CallSamplingResult<out T> {
    data class Success<T>(val value: T) : CallSamplingResult<T>
    data class Failure(val error: CallSamplingError) : CallSamplingResult<Nothing>
}
sealed interface CallSamplingError {
    data object Disabled : CallSamplingError
    data object NotInstalled : CallSamplingError
    data class MissingRequirements(val requirements: CallSamplingRequirements) : CallSamplingError
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
    suspend fun startSession(callType: CallType, callSource: CallSource, intervalSeconds: Int): CallSession
    suspend fun insertSample(sessionId: String, sampledAtUtcMs: Long, elapsedMs: Long, cellInfo: CellInfo)
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
    suspend fun setUploadsEnabled(enabled: Boolean, intervalMinutes: Long = 60, wifiOnly: Boolean = true): CallSamplingResult<Unit>
    suspend fun uploadPending(limit: Int = 50): CallSamplingResult<Int>
    suspend fun enqueueUploadNow(): CallSamplingResult<Unit>
    suspend fun exportSessions(lastN: Int = 100): CallSamplingResult<Uri>
    suspend fun deleteAll(): CallSamplingResult<Unit>
}
