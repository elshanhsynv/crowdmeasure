package com.example.crowdmeasure.presentation.screens.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.SetCollectionEnabledUseCase
import com.example.crowdmeasure.domain.usecase.SetConsentAcceptedUseCase
import com.example.crowdmeasure.domain.usecase.SetConsentGateDismissedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for consent/permission gate screen.
 *
 * Responsibilities:
 * - Expose user settings (consent, collection enabled)
 * - Handle consent acceptance
 * - Handle collection toggle
 * - Coordinate with domain layer
 *
 * State:
 * - Settings flow is shared and cached for 5 seconds after last subscriber
 * - All mutations happen through use cases (single source of truth)
 *
 * Performance:
 * - StateFlow prevents unnecessary recomposition
 * - Use cases handle background thread switching
 * - 5-second cache prevents redundant DB queries when rotating screen
 */
@HiltViewModel
class ConsentGateViewModel @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val setConsentAcceptedUseCase: SetConsentAcceptedUseCase,
    private val setCollectionEnabledUseCase: SetCollectionEnabledUseCase,
    private val setConsentGateDismissedUseCase: SetConsentGateDismissedUseCase
) : ViewModel() {

    /**
     * User settings state.
     *
     * - Null initially (loading)
     * - Non-null once loaded from repository
     * - Updates automatically when changed
     * - Survives configuration changes
     */
    val settings: StateFlow<AppSettings?> = userSessionRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null
        )

    /**
     * Accept or revoke consent.
     *
     * Note: Revoking consent should also disable collection per privacy laws.
     * If the user revokes consent, we should stop all data collection immediately.
     *
     * @param accepted true to accept consent, false to revoke
     */
    fun setConsent(accepted: Boolean) {
        viewModelScope.launch {
            setConsentAcceptedUseCase(accepted)

            // Privacy requirement: if consent is revoked, disable collection
            if (!accepted) {
                setCollectionEnabledUseCase(false)
            }
        }
    }

    /**
     * Enable or disable data collection.
     *
     * Note: Collection can only be enabled if consent is accepted.
     * The UI should enforce this, but we double-check here.
     *
     * @param enabled true to enable collection, false to disable
     */
    fun setCollection(enabled: Boolean) {
        viewModelScope.launch {
            // Safety check: can't enable collection without consent
            val currentSettings = settings.value
            if (enabled && currentSettings?.consentAccepted != true) {
                // Log this as a potential bug (should be caught by UI)
                return@launch
            }

            setCollectionEnabledUseCase(enabled)
        }
    }

    fun markConsentGateCompleted() {
        viewModelScope.launch {
            setConsentGateDismissedUseCase(true)
        }
    }

    /**
     * Marks the consent gate as dismissed by the user.
     * This prevents the gate from showing again even if they haven't completed setup.
     *
     * Note: This should be called when the user explicitly dismisses (not when completing).
     */
    fun dismissGate() {
        viewModelScope.launch {
            setConsentGateDismissedUseCase(true)
        }
    }
}