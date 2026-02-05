package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.example.crowdmeasure.data.export.ShareUtils
import com.example.crowdmeasure.data.prefs.WorkerStatusStore
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.DeleteAllDataUseCase
import com.example.crowdmeasure.domain.usecase.ExportMeasurementsUseCase
import com.example.crowdmeasure.domain.usecase.SetAutoRunUseCase
import com.example.crowdmeasure.domain.usecase.SetCollectionEnabledUseCase
import com.example.crowdmeasure.domain.usecase.SetCollectOnlyWifiUseCase
import com.example.crowdmeasure.domain.usecase.SetConsentAcceptedUseCase
import com.example.crowdmeasure.domain.usecase.SetEndpointUrlUseCase
import com.example.crowdmeasure.domain.usecase.SetFirestoreUploadsEnabledUseCase
import com.example.crowdmeasure.presentation.util.UiState
import com.example.crowdmeasure.workers.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val setEndpointUseCase: SetEndpointUrlUseCase,
    private val setCollectOnlyWifiUseCase: SetCollectOnlyWifiUseCase,
    private val setAutoRunUseCase: SetAutoRunUseCase,
    private val setConsentAcceptedUseCase: SetConsentAcceptedUseCase,
    private val setCollectionEnabledUseCase: SetCollectionEnabledUseCase,
    private val setFirestoreUploadsEnabledUseCase: SetFirestoreUploadsEnabledUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val exportMeasurementsUseCase: ExportMeasurementsUseCase,
    private val workScheduler: WorkScheduler,
    private val workerStatusStore: WorkerStatusStore,
) : ViewModel() {
    private val timeFormatter = DateTimeFormatter
        .ofPattern("MMM dd • HH:mm")
        .withZone(ZoneId.systemDefault())

    private val _exportState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    val settings: StateFlow<AppSettings?> = userSessionRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )
    val backgroundWorkState: StateFlow<BackgroundWorkUiState> = combine(
        settings,
        workScheduler.observeAutoRunWorkInfo(),
        workerStatusStore.autoRunStatus
    ) { userSettings, workInfo, workerStatus ->

        val workManagerStateLabel = workInfo?.state?.toHumanLabel() ?: "Not scheduled"

        val intervalLabel = userSettings?.autoRunIntervalMinutes
            ?.coerceAtLeast(15)
            ?.let { "$it min" }
            ?: "—"

        val lastStartLabel = formatTimestamp(workerStatus.lastStartUtcMs)
        val lastEndLabel = formatTimestamp(workerStatus.lastEndUtcMs)
        val lastResultLabel = workerStatus.lastResult?.takeIf { it.isNotBlank() } ?: "—"
        val lastUploadedLabel = workerStatus.lastUploadedCount.toString()
        val lastErrorLabel = workerStatus.lastError
            ?.takeIf { it.isNotBlank() }
            ?.let(::sanitizeError)
            ?: "None"

        val canRun = userSettings?.consentAccepted == true &&
                userSettings.collectionEnabled &&
                userSettings.autoRunEnabled

        BackgroundWorkUiState(
            workManagerStateLabel = workManagerStateLabel,
            intervalMinutesLabel = intervalLabel,
            lastStartLabel = lastStartLabel,
            lastEndLabel = lastEndLabel,
            lastResultLabel = lastResultLabel,
            lastUploadedLabel = lastUploadedLabel,
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
    val deleteState: StateFlow<UiState<Unit>> = _deleteState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = UiState.Idle
        )

    fun setConsent(accepted: Boolean) {
        viewModelScope.launch {
            setConsentAcceptedUseCase(accepted)
            if (!accepted) {
                workScheduler.cancelAutoRun()
            }
        }
    }

    fun setCollection(enabled: Boolean) {
        viewModelScope.launch {
            setCollectionEnabledUseCase(enabled)
            if (!enabled) {
                workScheduler.cancelAutoRun()
            }
        }
    }

    fun setFirestoreUploads(enabled: Boolean) {
        viewModelScope.launch {
            setFirestoreUploadsEnabledUseCase(enabled)
        }
    }
    fun saveEndpoint(url: String) {
        viewModelScope.launch {
            setEndpointUseCase(url)
        }
    }

    fun setCollectOnlyWifi(enabled: Boolean) {
        viewModelScope.launch {
            setCollectOnlyWifiUseCase(enabled)
            val currentSettings = settings.value ?: return@launch
            if (currentSettings.autoRunEnabled && currentSettings.consentAccepted && currentSettings.collectionEnabled) {
                workScheduler.scheduleAutoRun(
                    intervalMinutes = currentSettings.autoRunIntervalMinutes.toLong(),
                    wifiOnly = enabled
                )
            }
        }
    }

    fun setAutoRun(enabled: Boolean, intervalMinutes: Int) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val allowed = currentSettings?.consentAccepted == true &&
                    currentSettings.collectionEnabled

            val safeInterval = intervalMinutes.coerceIn(15, 10_080)

            if (!allowed) {
                setAutoRunUseCase(false, safeInterval)
                workScheduler.cancelAutoRun()
                return@launch
            }

            setAutoRunUseCase(enabled, safeInterval)

            if (enabled) {
                workScheduler.scheduleAutoRun(
                    intervalMinutes = safeInterval.toLong(),
                    wifiOnly = currentSettings?.collectOnlyWifi == true
                )
            } else {
                workScheduler.cancelAutoRun()
            }
        }
    }

    fun runAutoRunNow() {
        workScheduler.runAutoRunOnceNowDebug(ignoreConstraints = true)
    }

    fun rescheduleBackgroundWork() {
        workScheduler.enqueueRescheduleWorker()
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
            .replace(Regex("measurement[_-]?id\\s*[:=]\\s*\\S+", RegexOption.IGNORE_CASE), "ID=<redacted>")
            .replace(Regex("https?://\\S+"), "<url>")
    }
}


private fun WorkInfo.State.toHumanLabel(): String = when (this) {
    WorkInfo.State.ENQUEUED -> "Queued"
    WorkInfo.State.RUNNING -> "Running"
    WorkInfo.State.SUCCEEDED -> "Completed"
    WorkInfo.State.FAILED -> "Failed"
    WorkInfo.State.BLOCKED -> "Blocked"
    WorkInfo.State.CANCELLED -> "Cancelled"
}