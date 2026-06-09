package com.yourcompany.crowdmeasure.sdk.background

import kotlinx.coroutines.flow.Flow

data class BackgroundCollectionSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = CrowdMeasureBackground.DEFAULT_INTERVAL_MINUTES,
    val wifiOnly: Boolean = false,
)

data class BackgroundCollectionStatus(
    val settings: BackgroundCollectionSettings,
    val workState: BackgroundWorkState,
    val lastRun: BackgroundRun?,
)

data class BackgroundRun(
    val completedAtUtcMs: Long,
    val outcome: BackgroundRunOutcome,
    val code: BackgroundRunCode,
    val measurementId: String?,
)

enum class BackgroundWorkState { DISABLED, ENQUEUED, RUNNING, BLOCKED, SUCCEEDED, FAILED, CANCELLED, UNKNOWN }
enum class BackgroundRunOutcome { SUCCESS, SKIPPED, RETRYING, FAILURE }
enum class BackgroundRunCode {
    OK,
    SKIPPED_RECENT_RUN,
    SKIPPED_CONCURRENT_RUN,
    NOT_INSTALLED,
    COLLECTION_FAILED,
    CLEANUP_FAILED,
    UNEXPECTED_ERROR,
}

sealed interface BackgroundResult<out T> {
    data class Success<T>(val value: T) : BackgroundResult<T>
    data class Failure(val error: BackgroundError) : BackgroundResult<Nothing>
}

sealed interface BackgroundError {
    data class InvalidInterval(val intervalMinutes: Long) : BackgroundError
    data object NotEnabled : BackgroundError
    data class SchedulingFailed(val cause: Throwable) : BackgroundError
}

interface BackgroundCollectionClient {
    suspend fun enable(
        intervalMinutes: Long = CrowdMeasureBackground.DEFAULT_INTERVAL_MINUTES,
        wifiOnly: Boolean = false,
    ): BackgroundResult<Unit>
    suspend fun disable(): BackgroundResult<Unit>
    suspend fun enqueueRunNow(): BackgroundResult<Unit>
    suspend fun reschedule(): BackgroundResult<Unit>
    fun observeSettings(): Flow<BackgroundCollectionSettings>
    fun observeStatus(): Flow<BackgroundCollectionStatus>
}
