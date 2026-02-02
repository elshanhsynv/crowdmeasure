package com.example.crowdmeasure.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.data.export.ShareUtils
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.example.crowdmeasure.domain.usecase.DeleteAllDataUseCase
import com.example.crowdmeasure.domain.usecase.ExportMeasurementsUseCase
import com.example.crowdmeasure.domain.usecase.SetAutoRunUseCase
import com.example.crowdmeasure.domain.usecase.SetCollectOnlyWifiUseCase
import com.example.crowdmeasure.domain.usecase.SetCollectionEnabledUseCase
import com.example.crowdmeasure.domain.usecase.SetConsentAcceptedUseCase
import com.example.crowdmeasure.domain.usecase.SetEndpointUrlUseCase
import com.example.crowdmeasure.presentation.util.UiState
import com.example.crowdmeasure.workers.WorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: UserSessionRepository,
    private val setEndpoint: SetEndpointUrlUseCase,
    private val setWifiOnly: SetCollectOnlyWifiUseCase,
    private val setAutoRun: SetAutoRunUseCase,
    private val setConsentAccepted: SetConsentAcceptedUseCase,
    private val setCollectionEnabled: SetCollectionEnabledUseCase,
    private val deleteAll: DeleteAllDataUseCase,
    private val exportUseCase: ExportMeasurementsUseCase,
    private val scheduler: WorkScheduler
) : ViewModel() {

    val settings = session.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _exportState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val exportState = _exportState.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Idle)

    fun saveEndpoint(url: String) = viewModelScope.launch { setEndpoint(url) }
    fun setCollectOnlyWifi(enabled: Boolean) = viewModelScope.launch { setWifiOnly(enabled) }

    fun setConsent(accepted: Boolean) = viewModelScope.launch {
        setConsentAccepted(accepted)
        if (!accepted) {
            // Privacy: revoke background work too
            scheduler.cancelAutoRun()
        }
    }

    fun setCollection(enabled: Boolean) = viewModelScope.launch {
        setCollectionEnabled(enabled)
        if (!enabled) {
            // If user disables collection, background collection must stop.
            scheduler.cancelAutoRun()
        }
    }

    fun setAutoRunEnabled(enabled: Boolean, hours: Int) = viewModelScope.launch {
        // Guard: only allow auto-run if consent + collection enabled
        val s = settings.value
        val allowed = (s?.consentAccepted == true && s.collectionEnabled)
        if (!allowed) {
            setAutoRun(false, hours)
            scheduler.cancelAutoRun()
            return@launch
        }

        setAutoRun(enabled, hours)
        val wifiOnly = s?.collectOnlyWifi ?: false
        if (enabled) scheduler.scheduleAutoRun(hours, wifiOnly) else scheduler.cancelAutoRun()
    }

    fun ensureMaintenanceScheduled() {
        scheduler.scheduleMaintenanceDaily()
    }

    fun deleteMyData() = viewModelScope.launch {
        deleteAll()
    }

    fun exportLastN(context: Context, n: Int) = viewModelScope.launch {
        _exportState.value = UiState.Loading
        val res = exportUseCase(n)
        res.fold(
            onSuccess = { file ->
                ShareUtils.shareJson(context, file)
                _exportState.value = UiState.Success(Unit)
            },
            onFailure = { e ->
                _exportState.value = UiState.Error(e.message ?: "Export failed")
            }
        )
    }
}