package com.crowdmeasure.sdk.calls.upload

import com.crowdmeasure.sdk.calls.CallInstallationIdProvider
import com.crowdmeasure.sdk.calls.CallSamplingError
import com.crowdmeasure.sdk.calls.CallSamplingResult
import com.crowdmeasure.sdk.calls.CallUploader
import kotlinx.coroutines.flow.Flow

data class CallUploadConfig(
    val preferencesName: String = "crowdmeasure_sdk_calls_upload",
    val defaultBatchSize: Int = 50,
    val defaultIntervalMinutes: Long = 60,
    val defaultWifiOnly: Boolean = true,
    val uploader: CallUploader,
    val installationIdProvider: CallInstallationIdProvider? = null,
) {
    init {
        require(preferencesName.isNotBlank()) { "preferencesName must not be blank" }
        require(defaultBatchSize in 1..400) { "defaultBatchSize must be between 1 and 400" }
        require(defaultIntervalMinutes in 20..10_080) { "defaultIntervalMinutes must be between 20 minutes and 7 days" }
    }
}

data class CallUploadSettings(
    val enabled: Boolean = false,
    val intervalMinutes: Long = 60,
    val wifiOnly: Boolean = true,
)

enum class CallUploadWorkState { NOT_INSTALLED, DISABLED, ENQUEUED, RUNNING, BLOCKED, SUCCEEDED, FAILED, CANCELLED, UNKNOWN }

data class CallUploadStatus(
    val settings: CallUploadSettings,
    val workState: CallUploadWorkState,
)

interface CallUploadClient {
    suspend fun enable(intervalMinutes: Long = 60, wifiOnly: Boolean = true): CallSamplingResult<Unit>
    suspend fun disable(): CallSamplingResult<Unit>
    suspend fun uploadPending(limit: Int? = null): CallSamplingResult<Int>
    suspend fun enqueueUploadNow(): CallSamplingResult<Unit>
    suspend fun reschedule(): CallSamplingResult<Unit>
    fun observeSettings(): Flow<CallUploadSettings>
    fun observeStatus(): Flow<CallUploadStatus>
}
