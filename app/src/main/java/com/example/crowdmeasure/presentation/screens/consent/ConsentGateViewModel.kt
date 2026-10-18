package com.example.crowdmeasure.presentation.screens.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.AcceptConsentUseCase
import com.example.crowdmeasure.domain.usecase.DeleteAllDataUseCase
import com.example.crowdmeasure.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentGateViewModel @Inject constructor(
    userSessionRepository: UserSessionRepository,
    private val acceptConsentUseCase: AcceptConsentUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = userSessionRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )

    private val _deleteState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteState: StateFlow<UiState<Unit>> = _deleteState.asStateFlow()

    fun markConsentGateCompleted() {
        viewModelScope.launch { acceptConsentUseCase() }
    }

    /** Lets a user who has not (re-)accepted consent remove data from an earlier consent. */
    fun deleteAllData() {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            _deleteState.value = deleteAllDataUseCase().fold(
                onSuccess = { UiState.Success(Unit) },
                onFailure = { UiState.Error(message = "Delete failed. Try again.", throwable = it) }
            )
        }
    }
}
