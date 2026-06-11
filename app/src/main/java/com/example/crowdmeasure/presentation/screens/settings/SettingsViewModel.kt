package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.data.export.ShareUtils
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.DeleteAllDataUseCase
import com.example.crowdmeasure.domain.usecase.ExportCallSessionsUseCase
import com.example.crowdmeasure.domain.usecase.ExportMeasurementsUseCase
import com.example.crowdmeasure.domain.usecase.SetEndpointUrlUseCase
import com.example.crowdmeasure.presentation.util.UiState
import com.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.crowdmeasure.sdk.background.BackgroundCollectionStatus
import com.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.crowdmeasure.sdk.upload.MeasurementUploadStatus
import com.crowdmeasure.sdk.calls.CallSamplingClient
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
    private val background: BackgroundCollectionClient,
    private val uploads: MeasurementUploadClient,
    private val calls: CallSamplingClient,
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
    val backgroundWorkState: StateFlow<BackgroundWorkUiState> = combine(
        settings,
        background.observeStatus(),
        uploads.observeStatus(),
    ) { userSettings, backgroundStatus, uploadStatus ->
        val queueCounts = uploadStatus.queue

        val autoRunStateLabel = backgroundStatus.workState.name
        val uploadStateLabel = uploadStatus.workState.name
        val workManagerStateLabel = "Auto-run: $autoRunStateLabel / Upload: $uploadStateLabel"
        val nextScheduledWorkStateLabel = nextWorkStateLabel(backgroundStatus, uploadStatus)

        val intervalLabel = "${backgroundStatus.settings.intervalMinutes} min"

        val lastRun = backgroundStatus.lastRun
        val lastStartLabel = "—"
        val lastEndLabel = formatTimestamp(lastRun?.completedAtUtcMs ?: 0L)
        val lastResultLabel = lastRun?.outcome?.name ?: "—"
        val autoRunLastCodeLabel = lastRun?.code?.name ?: "—"
        val lastSuccessfulCollectionLabel = if (lastRun?.outcome?.name == "SUCCESS") {
            formatTimestamp(lastRun.completedAtUtcMs)
        } else "—"
        val lastMeasurementLabel = lastRun?.measurementId
            ?.takeIf { it.isNotBlank() }
            ?.let { id -> "${formatTimestamp(lastRun.completedAtUtcMs)} • ${id.take(12)}" }
            ?: "—"

        val uploadRun = uploadStatus.lastRun
        val uploadLastSuccessfulLabel = if (uploadRun?.outcome?.name == "SUCCESS") formatTimestamp(uploadRun.completedAtUtcMs) else "—"
        val uploadLastStartLabel = "—"
        val uploadLastEndLabel = formatTimestamp(uploadRun?.completedAtUtcMs ?: 0L)
        val uploadLastResultLabel = uploadRun?.outcome?.name ?: "—"
        val uploadLastCodeLabel = uploadRun?.code?.name ?: "—"
        val lastUploadedLabel = (uploadRun?.uploadedCount ?: 0).toString()
        val pendingRecordsLabel = queueCounts.pendingCount.toString()
        val failedRecordsLabel = queueCounts.failedCount.toString()
        val lastErrorLabel = uploadRun?.errorMessage
            ?.takeIf { it.isNotBlank() }
            ?.let(::sanitizeError)
            ?: "None"

        val canRun = backgroundStatus.settings.enabled

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
        calls.observeStatus().map { status ->
            val reason = status.lastMissedStart?.code?.name
            val timestamp = formatTimestamp(status.lastMissedStart?.atUtcMs ?: 0)
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
        viewModelScope.launch { background.enqueueRunNow() }
    }

    fun rescheduleBackgroundWork() {
        viewModelScope.launch {
            background.reschedule()
            uploads.reschedule()
            calls.activateEnabledFeatures()
        }
    }

    fun setCallSamplingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSessionRepository.setCallSamplingEnabled(enabled)
            calls.setCellularSamplingEnabled(enabled)
        }
    }

    fun setVoipCallSamplingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userSessionRepository.setVoipCallSamplingEnabled(enabled)
            calls.setVoipSamplingEnabled(enabled)
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
        viewModelScope.launch { background.reschedule() }
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

private fun nextWorkStateLabel(backgroundStatus: BackgroundCollectionStatus, uploadStatus: MeasurementUploadStatus): String {
    val active = listOfNotNull(
        "Auto-run: ${backgroundStatus.workState.name}",
        "Upload: ${uploadStatus.workState.name}"
    )
    return active.firstOrNull() ?: "Not scheduled"
}
