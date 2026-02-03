package com.example.crowdmeasure.presentation.screens.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.SetCollectionEnabledUseCase
import com.example.crowdmeasure.domain.usecase.SetConsentAcceptedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConsentGateViewModel @Inject constructor(
    session: UserSessionRepository,
    private val setConsentAccepted: SetConsentAcceptedUseCase,
    private val setCollectionEnabled: SetCollectionEnabledUseCase
) : ViewModel() {

    val settings = session.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setConsent(accepted: Boolean) = viewModelScope.launch {
        setConsentAccepted(accepted)
    }

    fun setCollection(enabled: Boolean) = viewModelScope.launch {
        setCollectionEnabled(enabled)
    }
}