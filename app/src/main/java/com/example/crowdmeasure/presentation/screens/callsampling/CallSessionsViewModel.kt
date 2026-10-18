package com.example.crowdmeasure.presentation.screens.callsampling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdmeasure.domain.model.CallCellSample
import com.example.crowdmeasure.domain.model.CallSession
import com.crowdmeasure.sdk.calls.CallSamplingClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CallSessionsViewModel @Inject constructor(
    private val calls: CallSamplingClient
) : ViewModel() {
    private val selectedSessionId = MutableStateFlow<String?>(null)

    val sessions: StateFlow<List<CallSession>> = calls.observeSessions()
        .catch { error ->
            Timber.e(error, "Call sessions could not be loaded")
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val samples: StateFlow<List<CallCellSample>> = selectedSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList()) else calls.observeSamples(sessionId)
        }
        .catch { error ->
            Timber.e(error, "Call samples could not be loaded")
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList()
        )

    fun selectSession(sessionId: String) {
        selectedSessionId.value = sessionId
    }

    fun clearData() {
        viewModelScope.launch {
            calls.deleteAll()
            selectedSessionId.value = null
        }
    }
}
