package com.example.crowdmeasure.presentation.screens.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.SetConsentGateDismissedUseCase
import com.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.crowdmeasure.sdk.calls.CallSamplingClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentGateViewModel @Inject constructor(
    userSessionRepository: UserSessionRepository,
    private val setConsentGateDismissedUseCase: SetConsentGateDismissedUseCase,
    private val background: BackgroundCollectionClient,
    private val calls: CallSamplingClient,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = userSessionRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )


    fun markConsentGateCompleted() {
        viewModelScope.launch {
            setConsentGateDismissedUseCase(true)
            rescheduleBackgroundWork()
        }
    }

    private fun rescheduleBackgroundWork() {
        viewModelScope.launch {
            background.reschedule()
            calls.activateEnabledFeatures()
        }
    }
}
