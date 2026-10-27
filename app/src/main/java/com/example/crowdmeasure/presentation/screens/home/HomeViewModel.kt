package com.example.crowdmeasure.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crowdmeasure.sdk.model.Measurement
import com.example.crowdmeasure.domain.model.ArmedContinuousSession
import com.example.crowdmeasure.domain.model.ContinuousSessionStatus
import com.example.crowdmeasure.domain.model.DEFAULT_CONTINUOUS_SESSION_GAP_SECONDS
import com.example.crowdmeasure.domain.model.DEFAULT_CONTINUOUS_SESSION_MINUTES
import com.example.crowdmeasure.domain.repo.ContinuousSessionRepository
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.RunMeasurementUseCase
import com.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.crowdmeasure.sdk.upload.MeasurementUploadResult
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.AppLog
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runMeasurementUseCase: RunMeasurementUseCase,
    private val measurementRepository: MeasurementRepository,
    private val continuousSessions: ContinuousSessionRepository,
    private val uploads: MeasurementUploadClient,
    userSessionRepository: UserSessionRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private var currentMeasurementJob: Job? = null
    private val measurementState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    private val uploadState = MutableStateFlow<UiState<Int>>(UiState.Idle)
    private val selectedSessionMinutes = MutableStateFlow(DEFAULT_CONTINUOUS_SESSION_MINUTES)
    private val selectedSessionGapSeconds = MutableStateFlow(DEFAULT_CONTINUOUS_SESSION_GAP_SECONDS)

    private val sessionState = combine(
        continuousSessions.observeStatus(),
        continuousSessions.observeArmed(),
        selectedSessionMinutes,
        selectedSessionGapSeconds,
    ) { status, armed, minutes, gap -> SessionState(status, armed, minutes, gap) }

    private val baseState = combine(
        userSessionRepository.settings,
        measurementRepository.observeLastMeasurement(),
        uploads.observeQueue(),
        AppPermissions.locationServicesEnabledFlow(appContext),
    ) { settings, lastMeasurement, queue, locationServicesOn ->

        val uploadsEnabled = settings.firestoreUploadsEnabled

        HomeUiState(
            uploadsEnabled = uploadsEnabled,
            canCollect = settings.consentGateDismissed,
            locationServicesOn = locationServicesOn,
            queuedCount = queue.pendingCount + queue.failedCount,
            lastMeasurement = lastMeasurement?.toUiModel()
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseState,
        measurementState,
        uploadState,
        sessionState,
    ) { state, measurementOp, uploadOp, session ->
        state.copy(
            measurementState = measurementOp,
            uploadState = uploadOp,
            continuous = session.status,
            armedSession = session.armed,
            selectedSessionMinutes = session.minutes,
            selectedSessionGapSeconds = session.gapSeconds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState(
            locationServicesOn = AppPermissions.isLocationServicesEnabled(appContext)
        )
    )

    fun startMeasurement() {
        if (currentMeasurementJob?.isActive == true) return

        currentMeasurementJob = viewModelScope.launch {
            AppLog.i("Home", "Manual measurement started.")
            measurementState.value = UiState.Loading

            val result = runMeasurementUseCase()

            result.fold(
                onSuccess = { measurement ->
                    AppLog.i("Home", "Manual measurement collected: id=${measurement.meta.measurementId}")
                    // Saving to local DB
                    val saveResult = runCatching {
                        measurementRepository.insert(measurement)
                    }

                    measurementState.value = if (saveResult.isSuccess) {
                        AppLog.i("Home", "Manual measurement stored locally: id=${measurement.meta.measurementId}")
                        UiState.Success(Unit)
                    } else {
                        AppLog.e(
                            "Home",
                            "Manual measurement local save failed.",
                            saveResult.exceptionOrNull(),
                        )
                        UiState.Error(
                            message = "Measurement completed but couldn't save to local database.",
                            throwable = saveResult.exceptionOrNull()
                        )
                    }
                },
                onFailure = { error ->
                    AppLog.e("Home", "Manual measurement failed: ${error.message}", error)
                    measurementState.value = UiState.Error(
                        message = error.message ?: "Measurement failed. Check permissions and try again.",
                        throwable = error
                    )
                }
            )
        }
    }

    fun stopMeasurement() {
        currentMeasurementJob?.cancel()
        currentMeasurementJob = null
        measurementState.value = UiState.Idle
    }

    fun selectSessionDuration(minutes: Int) {
        selectedSessionMinutes.value = minutes
    }

    fun selectSessionGap(seconds: Int) {
        selectedSessionGapSeconds.value = seconds
    }

    fun startContinuousSession() {
        viewModelScope.launch {
            AppLog.i("Home", "Continuous session started for ${selectedSessionMinutes.value} minutes.")
            continuousSessions.start(selectedSessionMinutes.value, selectedSessionGapSeconds.value)
        }
    }

    fun stopContinuousSession() {
        viewModelScope.launch {
            AppLog.i("Home", "Continuous session stopped by user.")
            continuousSessions.stop()
        }
    }

    /** Starts a session of the selected length when the next measurement finishes. */
    fun armContinuousSession() {
        viewModelScope.launch {
            val latestId = measurementRepository.observeLastMeasurement().first()?.meta?.measurementId
            continuousSessions.arm(selectedSessionMinutes.value, selectedSessionGapSeconds.value, latestId)
        }
    }

    fun cancelArmedSession() {
        viewModelScope.launch { continuousSessions.disarm() }
    }

    fun uploadNow() {
        viewModelScope.launch {
            uploadState.value = UiState.Loading

            uploadState.value = when (val result = uploads.uploadNow()) {
                is MeasurementUploadResult.Success -> UiState.Success(result.value)
                is MeasurementUploadResult.Failure -> UiState.Error(
                        message = "Upload failed. Check your connection and try again.",
                        throwable = IllegalStateException(result.error.toString())
                    )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentMeasurementJob?.cancel()
    }
}

private data class SessionState(
    val status: ContinuousSessionStatus,
    val armed: ArmedContinuousSession?,
    val minutes: Int,
    val gapSeconds: Int,
)

private fun Measurement.toUiModel() = MeasurementUi(
    meta = this.meta,
    environment = this.environment,
    cell = this.environment.network.cell,
    wifi = this.environment.network.wifi,
    performance = this.performance,
)
