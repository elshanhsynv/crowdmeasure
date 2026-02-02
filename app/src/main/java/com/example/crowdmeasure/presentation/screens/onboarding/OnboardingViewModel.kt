package com.example.crowdmeasure.presentation.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val session: UserSessionRepository
) : ViewModel() {

    val settings = session.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setConsentAccepted(accepted: Boolean) = viewModelScope.launch {
        session.setConsentAccepted(accepted)
        // collection toggle defaults to same as consent acceptance only if accepted
        if (accepted) session.setCollectionEnabled(true)
        else session.setCollectionEnabled(false)
    }

    fun setCollectionEnabled(enabled: Boolean) = viewModelScope.launch {
        session.setCollectionEnabled(enabled)
    }
}