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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runMeasurementUseCase: RunMeasurementUseCase,
    private val measurementRepository: MeasurementRepository,
    private val uploadNowUseCase: UploadNowUseCase,
    userSessionRepository: UserSessionRepository
) : ViewModel() {

    private var currentMeasurementJob: Job? = null
    private val measurementState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    private val uploadState = MutableStateFlow<UiState<Int>>(UiState.Idle)

    val uiState: StateFlow<HomeUiState> = combine(
        userSessionRepository.settings,
        measurementRepository.observeLastMeasurement(),
        measurementRepository.observeQueueCount(),
        measurementState,
        uploadState
    ) { settings, lastMeasurement, queueCount, measurementOp, uploadOp ->

//        val consentAccepted = settings.consentAccepted
//        val collectionEnabled = settings.collectionEnabled
        val uploadsEnabled = settings.firestoreUploadsEnabled
//        val canCollect = consentAccepted && collectionEnabled

        HomeUiState(
//            consentAccepted = consentAccepted,
//            collectionEnabled = collectionEnabled,
            uploadsEnabled = uploadsEnabled,
            canCollect = true,
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

    fun stopMeasurement() {
        currentMeasurementJob?.cancel()
        currentMeasurementJob = null
        measurementState.value = UiState.Idle
    }

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

    fun clearMeasurementMessage() {
        if (measurementState.value is UiState.Success || measurementState.value is UiState.Error) {
            measurementState.value = UiState.Idle
        }
    }

    fun clearUploadMessage() {
        if (uploadState.value is UiState.Success || uploadState.value is UiState.Error) {
            uploadState.value = UiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentMeasurementJob?.cancel()
    }
}

private fun Measurement.toUiModel() = MeasurementUi(
    header = this.header,
    context = this.context,
    cell = this.cell,
    wifi = this.wifi,
    performance = this.performance,
    feedbackTag = this.feedbackTag
)