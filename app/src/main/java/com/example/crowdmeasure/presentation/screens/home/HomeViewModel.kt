package com.example.crowdmeasure.presentation.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourcompany.crowdmeasure.sdk.model.Measurement
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.RunMeasurementUseCase
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadClient
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadResult
import com.example.crowdmeasure.presentation.util.AppPermissions
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val uploads: MeasurementUploadClient,
    userSessionRepository: UserSessionRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val appContext = context.applicationContext
    private var currentMeasurementJob: Job? = null
    private val measurementState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    private val uploadState = MutableStateFlow<UiState<Int>>(UiState.Idle)

    private val baseState = combine(
        userSessionRepository.settings,
        measurementRepository.observeLastMeasurement(),
        uploads.observeQueue(),
        AppPermissions.locationServicesEnabledFlow(appContext),
    ) { settings, lastMeasurement, queue, locationServicesOn ->

        val uploadsEnabled = settings.firestoreUploadsEnabled

        HomeUiState(
            uploadsEnabled = uploadsEnabled,
            canCollect = true,
            locationServicesOn = locationServicesOn,
            queuedCount = queue.pendingCount + queue.failedCount,
            lastMeasurement = lastMeasurement?.toUiModel()
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseState,
        measurementState,
        uploadState
    ) { state, measurementOp, uploadOp ->
        state.copy(
            measurementState = measurementOp,
            uploadState = uploadOp
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
            measurementState.value = UiState.Loading

            val result = runMeasurementUseCase()

            result.fold(
                onSuccess = { measurement ->
                    // Saving to local DB
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

            uploadState.value = when (val result = uploads.uploadNow()) {
                is MeasurementUploadResult.Success -> UiState.Success(result.value)
                is MeasurementUploadResult.Failure -> UiState.Error(
                        message = "Upload failed. Check your connection and try again.",
                        throwable = IllegalStateException(result.error.toString())
                    )
            }
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
    meta = this.meta,
    environment = this.environment,
    cell = this.environment.network.cell,
    wifi = this.environment.network.wifi,
    performance = this.performance,
)
