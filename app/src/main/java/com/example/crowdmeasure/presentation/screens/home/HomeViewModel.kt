package com.example.crowdmeasure.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.RunMeasurementUseCase
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val runMeasurement: RunMeasurementUseCase,
    private val repo: MeasurementRepository,
    private val uploadNow: UploadNowUseCase,
    session: UserSessionRepository
) : ViewModel() {

    val settings = session.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val last = repo.observeLastMeasurement().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val queueCount = repo.observeQueueCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var currentJob: Job? = null
    private val _runState = kotlinx.coroutines.flow.MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val runState = _runState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Idle)

    private val _uploadState = MutableStateFlow<UiState<Int>>(UiState.Idle)
    val uploadState: StateFlow<UiState<Int>> = _uploadState.asStateFlow()

    fun startMeasurement() {
        if (currentJob?.isActive == true) return
        currentJob = viewModelScope.launch {
            _runState.value = UiState.Loading
            val res = runMeasurement()
            res.fold(
                onSuccess = { m ->
                    runCatching { repo.insert(m) }
                        .onSuccess { _runState.value = UiState.Success(Unit) }
                        .onFailure { e -> _runState.value = UiState.Error(e.message ?: "Save failed", e) }
                },
                onFailure = { e ->
                    _runState.value = UiState.Error(e.message ?: "Measurement failed", e)
                }
            )
        }
    }

    fun stopMeasurement() {
        currentJob?.cancel()
        currentJob = null
        _runState.value = UiState.Idle
    }

    fun uploadNow() = viewModelScope.launch {
        _uploadState.value = UiState.Loading
        val res = uploadNow()
        res.fold(
            onSuccess = { count -> _uploadState.value = UiState.Success(count) },
            onFailure = { e -> _uploadState.value = UiState.Error(e.message ?: "Upload failed") }
        )
    }
}