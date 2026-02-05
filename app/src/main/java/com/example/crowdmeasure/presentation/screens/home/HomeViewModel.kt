package com.example.crowdmeasure.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.model.Measurement
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.RunMeasurementUseCase
import com.example.crowdmeasure.domain.usecase.UploadNowUseCase
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home screen - main dashboard for running measurements.
 *
 * Responsibilities:
 * - Run single measurement on demand
 * - Show last measurement preview
 * - Show upload queue status
 * - Trigger manual upload
 * - Combine all relevant state into single UI state
 *
 * Performance:
 * - StateFlow with 5-second cache (survives config changes)
 * - Stable state combining (only recombines when sources change)
 * - Cancellable measurement job
 *
 * State Management:
 * - All sources are flows (reactive)
 * - UI state is derived, not duplicated
 * - Operations use use cases (domain layer)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runMeasurementUseCase: RunMeasurementUseCase,
    private val measurementRepository: MeasurementRepository,
    private val uploadNowUseCase: UploadNowUseCase,
    userSessionRepository: UserSessionRepository
) : ViewModel() {

    // ═══════════════════════════════════════════════════════════
    // Private State
    // ═══════════════════════════════════════════════════════════

    /**
     * Current measurement job (for cancellation).
     * Null when no measurement is running.
     */
    private var currentMeasurementJob: Job? = null

    /**
     * State of the current measurement operation.
     */
    private val measurementState = MutableStateFlow<UiState<Unit>>(UiState.Idle)

    /**
     * State of the current upload operation.
     */
    private val uploadState = MutableStateFlow<UiState<Int>>(UiState.Idle)

    // ═══════════════════════════════════════════════════════════
    // Public State
    // ═══════════════════════════════════════════════════════════

    /**
     * Combined UI state for the home screen.
     *
     * Sources:
     * - User settings (consent, collection enabled, uploads enabled)
     * - Last measurement (preview)
     * - Queue count (pending uploads)
     * - Measurement operation state
     * - Upload operation state
     *
     * Updates automatically when any source changes.
     * Cached for 5 seconds after last subscriber (config change resilience).
     */
    val uiState: StateFlow<HomeUiState> = combine(
        userSessionRepository.settings,
        measurementRepository.observeLastMeasurement(),
        measurementRepository.observeQueueCount(),
        measurementState,
        uploadState
    ) { settings, lastMeasurement, queueCount, measurementOp, uploadOp ->

        val consentAccepted = settings.consentAccepted
        val collectionEnabled = settings.collectionEnabled
        val uploadsEnabled = settings.firestoreUploadsEnabled
        val canCollect = consentAccepted && collectionEnabled

        HomeUiState(
            consentAccepted = consentAccepted,
            collectionEnabled = collectionEnabled,
            uploadsEnabled = uploadsEnabled,
            canCollect = canCollect,
            queuedCount = queueCount,
            measurementState = measurementOp,
            uploadState = uploadOp,
            lastMeasurement = lastMeasurement?.toUiModel()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState()
    )

    // ═══════════════════════════════════════════════════════════
    // Actions
    // ═══════════════════════════════════════════════════════════

    /**
     * Start a single measurement.
     *
     * Flow:
     * 1. Set state to Loading
     * 2. Run measurement via use case
     * 3. Save to local repository
     * 4. Update state (Success or Error)
     *
     * Note: Prevents concurrent measurements (only one at a time).
     */
    fun startMeasurement() {
        // Prevent concurrent measurements
        if (currentMeasurementJob?.isActive == true) return

        currentMeasurementJob = viewModelScope.launch {
            measurementState.value = UiState.Loading

            val result = runMeasurementUseCase()

            result.fold(
                onSuccess = { measurement ->
                    // Save to local DB
                    val saveResult = runCatching {
                        measurementRepository.insert(measurement)
                    }

                    measurementState.value = if (saveResult.isSuccess) {
                        UiState.Success(Unit)
                    } else {
                        UiState.Error(
                            message = "Measurement completed but couldn't save to local database.",
                            throwable = saveResult.exceptionOrNull()
                        )
                    }
                },
                onFailure = { error ->
                    measurementState.value = UiState.Error(
                        message = "Measurement failed. Check permissions and try again.",
                        throwable = error
                    )
                }
            )
        }
    }

    /**
     * Stop the current measurement (if running).
     *
     * Note: This cancels the coroutine, which should trigger cleanup in the use case.
     */
    fun stopMeasurement() {
        currentMeasurementJob?.cancel()
        currentMeasurementJob = null
        measurementState.value = UiState.Idle
    }

    /**
     * Trigger immediate upload of queued measurements.
     *
     * Flow:
     * 1. Set state to Loading
     * 2. Upload via use case
     * 3. Update state with count or error
     *
     * Note: Use case handles network checks, batching, etc.
     */
    fun uploadNow() {
        viewModelScope.launch {
            uploadState.value = UiState.Loading

            val result = uploadNowUseCase()

            uploadState.value = result.fold(
                onSuccess = { uploadedCount ->
                    UiState.Success(uploadedCount)
                },
                onFailure = { error ->
                    UiState.Error(
                        message = "Upload failed. Check your connection and try again.",
                        throwable = error
                    )
                }
            )
        }
    }

    /**
     * Clear success message for measurement operation.
     * Call this after showing a success snackbar/toast.
     */
    fun clearMeasurementMessage() {
        if (measurementState.value is UiState.Success || measurementState.value is UiState.Error) {
            measurementState.value = UiState.Idle
        }
    }

    /**
     * Clear success/error message for upload operation.
     * Call this after showing a success snackbar/toast.
     */
    fun clearUploadMessage() {
        if (uploadState.value is UiState.Success || uploadState.value is UiState.Error) {
            uploadState.value = UiState.Idle
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Cleanup
    // ═══════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        // Cancel any running measurement
        currentMeasurementJob?.cancel()
    }
}

/**
 * Map domain Measurement to UI model.
 * Keeps UI layer decoupled from domain models.
 */
private fun Measurement.toUiModel() = MeasurementUi(
    header = this.header,
    context = this.context,
    cell = this.cell,
    wifi = this.wifi,
    performance = this.performance,
    feedbackTag = this.feedbackTag
)