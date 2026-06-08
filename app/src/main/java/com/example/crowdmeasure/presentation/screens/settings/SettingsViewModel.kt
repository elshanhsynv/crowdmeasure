package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.crowdmeasure.data.export.ShareUtils
import com.example.crowdmeasure.data.prefs.CallSamplingStatusStore
import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.DeleteAllDataUseCase
import com.example.crowdmeasure.domain.usecase.ExportCallSessionsUseCase
import com.example.crowdmeasure.domain.usecase.ExportMeasurementsUseCase
import com.example.crowdmeasure.domain.usecase.SetAutoRunUseCase
import com.example.crowdmeasure.domain.usecase.SetCollectOnlyWifiUseCase
import com.example.crowdmeasure.domain.usecase.SetEndpointUrlUseCase
import com.example.crowdmeasure.domain.usecase.SetFirestoreUploadsEnabledUseCase
import com.example.crowdmeasure.presentation.util.UiState
import com.example.crowdmeasure.workers.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val exportMeasurementsUseCase: ExportMeasurementsUseCase,
    private val exportCallSessionsUseCase: ExportCallSessionsUseCase,
    private val workScheduler: WorkScheduler,
    measurementRepository: MeasurementRepository,
    private val callSamplingStatusStore: CallSamplingStatusStore,
    workerStatusStore: WorkerStatusStore,
) : ViewModel() {
    private val timeFormatter = DateTimeFormatter
        .ofPattern("MMM dd • HH:mm")
        .withZone(ZoneId.systemDefault())

    private val _exportState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    private val _callExportState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    val settings: StateFlow<AppSettings?> = userSessionRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )
    private val workInfos = combine(
        workScheduler.observeAutoRunWorkInfo(),
        workScheduler.observeUploadWorkInfo()
    ) { autoRunInfo, uploadInfo ->
        WorkInfos(autoRunInfo = autoRunInfo, uploadInfo = uploadInfo)
    }

    private val queueCounts = combine(
        measurementRepository.observePendingCount(),
        measurementRepository.observeFailedCount()
    ) { pendingCount, failedCount ->
        QueueCounts(pendingCount = pendingCount, failedCount = failedCount)
    }

    private val workerStatuses = combine(
        workerStatusStore.autoRunStatus,
        workerStatusStore.uploadStatus,
        queueCounts
    ) { autoRunStatus, uploadStatus, queueCounts ->
        WorkerStatuses(
            autoRunStatus = autoRunStatus,
            uploadStatus = uploadStatus,
            queueCounts = queueCounts
        )
    }

    val backgroundWorkState: StateFlow<BackgroundWorkUiState> = combine(
        settings,
        workInfos,
        workerStatuses
    ) { userSettings, workInfos, workerStatuses ->
        val autoRunInfo = workInfos.autoRunInfo
        val uploadInfo = workInfos.uploadInfo
        val workerStatus = workerStatuses.autoRunStatus
        val uploadStatus = workerStatuses.uploadStatus
        val queueCounts = workerStatuses.queueCounts

        val autoRunStateLabel = autoRunInfo?.state?.toHumanLabel() ?: "Not scheduled"
        val uploadStateLabel = uploadInfo?.state?.toHumanLabel() ?: "Not scheduled"
        val workManagerStateLabel = "Auto-run: $autoRunStateLabel / Upload: $uploadStateLabel"
        val nextScheduledWorkStateLabel = nextWorkStateLabel(autoRunInfo, uploadInfo)

        val intervalLabel = userSettings?.autoRunIntervalMinutes
            ?.coerceAtLeast(20)
            ?.let { "$it min" }
            ?: "—"

        val lastStartLabel = formatTimestamp(workerStatus.lastStartUtcMs)
        val lastEndLabel = formatTimestamp(workerStatus.lastEndUtcMs)
        val lastResultLabel = workerStatus.lastResult?.takeIf { it.isNotBlank() } ?: "—"
        val autoRunLastCodeLabel = workerStatus.lastCode?.takeIf { it.isNotBlank() } ?: "—"
        val lastSuccessfulCollectionLabel = formatTimestamp(workerStatus.lastSuccessUtcMs)
        val lastMeasurementLabel = workerStatus.lastMeasurementId
            ?.takeIf { it.isNotBlank() }
            ?.let { id -> "${formatTimestamp(workerStatus.lastMeasurementTimestampUtcMs)} • ${id.take(12)}" }
            ?: "—"

        val uploadLastSuccessfulLabel = formatTimestamp(uploadStatus.lastSuccessUtcMs)
        val uploadLastStartLabel = formatTimestamp(uploadStatus.lastStartUtcMs)
        val uploadLastEndLabel = formatTimestamp(uploadStatus.lastEndUtcMs)
        val uploadLastResultLabel = uploadStatus.lastResult?.takeIf { it.isNotBlank() } ?: "—"
        val uploadLastCodeLabel = uploadStatus.lastCode?.takeIf { it.isNotBlank() } ?: "—"
        val lastUploadedLabel = uploadStatus.uploadedCount.toString()
        val pendingRecordsLabel = queueCounts.pendingCount.toString()
        val failedRecordsLabel = queueCounts.failedCount.toString()
        val lastErrorLabel = uploadStatus.lastError
            ?.takeIf { it.isNotBlank() }
            ?.let(::sanitizeError)
            ?: "None"

        val canRun = userSettings != null &&
                userSettings.autoRunEnabled

        BackgroundWorkUiState(
            workManagerStateLabel = workManagerStateLabel,
            nextScheduledWorkStateLabel = nextScheduledWorkStateLabel,
            intervalMinutesLabel = intervalLabel,
            lastStartLabel = lastStartLabel,
            lastEndLabel = lastEndLabel,
            lastResultLabel = lastResultLabel,
            autoRunLastCodeLabel = autoRunLastCodeLabel,
            autoRunLastSuccessfulCollectionLabel = lastSuccessfulCollectionLabel,
            autoRunLastMeasurementLabel = lastMeasurementLabel,
            uploadLastSuccessfulUploadLabel = uploadLastSuccessfulLabel,
            uploadLastStartLabel = uploadLastStartLabel,
            uploadLastEndLabel = uploadLastEndLabel,
            uploadLastResultLabel = uploadLastResultLabel,
            uploadLastCodeLabel = uploadLastCodeLabel,
            lastUploadedLabel = lastUploadedLabel,
            pendingRecordsLabel = pendingRecordsLabel,
            failedRecordsLabel = failedRecordsLabel,
            lastErrorLabel = lastErrorLabel,
            canRunNow = canRun,
            canReschedule = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = BackgroundWorkUiState.loading()
    )

    val exportState: StateFlow<UiState<Unit>> = _exportState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UiState.Idle
        )
    val callExportState: StateFlow<UiState<Unit>> = _callExportState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UiState.Idle
        )
    val deleteState: StateFlow<UiState<Unit>> = _deleteState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UiState.Idle
        )

    val callSamplingStatus: StateFlow<CallSamplingStatusUiState> =
        callSamplingStatusStore.status.map { status ->
            val reason = status.lastMissedReason?.takeIf { it.isNotBlank() }
            val timestamp = formatTimestamp(status.lastMissedAtUtcMs)
            CallSamplingStatusUiState(
                lastMissedLabel = if (reason == null) "None" else "$timestamp • $reason",
                voipMonitorActive = status.voipMonitorActive
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = CallSamplingStatusUiState.empty()
        )

    fun runAutoRunNow() {
        workScheduler.runAutoRunOnceNowDebug(ignoreConstraints = true)
    }

    fun rescheduleBackgroundWork() {
        workScheduler.enqueueRescheduleWorker()
    }

    fun setCallSamplingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSessionRepository.setCallSamplingEnabled(enabled)
        }
    }

    fun setVoipCallSamplingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSessionRepository.setVoipCallSamplingEnabled(enabled)
        }
    }

    fun exportData(context: Context, count: Int) {
        viewModelScope.launch {
            _exportState.value = UiState.Loading

            val safeCount = count.coerceIn(1, 10_000)

            exportMeasurementsUseCase(safeCount).fold(
                onSuccess = { file ->
                    ShareUtils.shareJson(context, file)
                    _exportState.value = UiState.Success(Unit)
                },
                onFailure = { error ->
                    _exportState.value = UiState.Error(
                        message = "Export failed. Try again.",
                        throwable = error
                    )
                }
            )
        }
    }

    fun clearExportState() {
        _exportState.value = UiState.Idle
    }

    fun exportCallData(context: Context, count: Int) {
        viewModelScope.launch {
            _callExportState.value = UiState.Loading

            val safeCount = count.coerceIn(1, 1_000)

            exportCallSessionsUseCase(safeCount).fold(
                onSuccess = { uri ->
                    ShareUtils.shareJson(
                        context = context,
                        uri = uri,
                        chooserTitle = "Share call sessions JSON"
                    )
                    _callExportState.value = UiState.Success(Unit)
                },
                onFailure = { error ->
                    _callExportState.value = UiState.Error(
                        message = "Call export failed. Try again.",
                        throwable = error
                    )
                }
            )
        }
    }

    fun clearCallExportState() {
        _callExportState.value = UiState.Idle
    }

    fun deleteAllData() {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading

            val result = deleteAllDataUseCase()

            result.fold(
                onSuccess = {
                    _deleteState.value = UiState.Success(Unit)
                },
                onFailure = { error ->
                    _deleteState.value = UiState.Error(
                        message = "Delete failed. Try again.",
                        throwable = error
                    )
                }
            )
        }
    }

    fun clearDeleteState() {
        _deleteState.value = UiState.Idle
    }

    fun ensureMaintenanceScheduled() {
        workScheduler.scheduleMaintenanceDaily()
    }

    private fun formatTimestamp(timestampMs: Long): String {
        return if (timestampMs > 0) {
            timeFormatter.format(Instant.ofEpochMilli(timestampMs))
        } else {
            "—"
        }
    }

    private fun sanitizeError(raw: String): String {
        return raw.take(120)
            .replace(
                Regex("measurement[_-]?id\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE),
                "ID=<redacted>"
            )
            .replace(Regex("https?://\\S+"), "<url>")
    }
}

private data class WorkInfos(
    val autoRunInfo: WorkInfo?,
    val uploadInfo: WorkInfo?
)

private data class QueueCounts(
    val pendingCount: Int,
    val failedCount: Int
)

private data class WorkerStatuses(
    val autoRunStatus: WorkerStatusStore.AutoRunStatus,
    val uploadStatus: WorkerStatusStore.UploadStatus,
    val queueCounts: QueueCounts
)

private fun nextWorkStateLabel(autoRunInfo: WorkInfo?, uploadInfo: WorkInfo?): String {
    val active = listOfNotNull(
        autoRunInfo?.let { "Auto-run: ${it.state.toHumanLabel()}" },
        uploadInfo?.let { "Upload: ${it.state.toHumanLabel()}" }
    )
    return active.firstOrNull() ?: "Not scheduled"
}

private fun WorkInfo.State.toHumanLabel(): String = when (this) {
    WorkInfo.State.ENQUEUED -> "ENQUEUED"
    WorkInfo.State.RUNNING -> "RUNNING"
    WorkInfo.State.SUCCEEDED -> "COMPLETED"
    WorkInfo.State.FAILED -> "FAILED"
    WorkInfo.State.BLOCKED -> "BLOCKED"
    WorkInfo.State.CANCELLED -> "CANCELLED"
}
