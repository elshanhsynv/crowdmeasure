package com.example.crowdmeasure.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdateUiState(
    val checking: Boolean = false,
    val update: UpdateMetadata? = null,
    val installing: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()
    private var startupCheckStarted = false

    fun checkOnStartup() {
        if (startupCheckStarted) return
        startupCheckStarted = true
        checkForUpdate(notify = true)
    }

    fun retryCheck() {
        checkForUpdate(notify = true)
    }

    fun dismissOptionalUpdate() {
        val update = _uiState.value.update
        if (update?.forceUpdate == true) return
        _uiState.update { it.copy(update = null, message = null, error = null) }
    }

    fun installUpdate() {
        val metadata = _uiState.value.update ?: return
        if (_uiState.value.installing) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    installing = true,
                    message = "Preparing update",
                    error = null
                )
            }

            updateRepository.downloadVerifyAndInstall(metadata).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            installing = false,
                            message = "Confirm installation in the system prompt.",
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    if (error is InstallPermissionRequiredException) {
                        updateRepository.openUnknownAppSourcesSettings()
                    }
                    _uiState.update {
                        it.copy(
                            installing = false,
                            message = null,
                            error = error.message ?: "Update failed. Try again."
                        )
                    }
                }
            )
        }
    }

    private fun checkForUpdate(notify: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(checking = true, error = null) }

            updateRepository.checkForUpdate(notify = notify).fold(
                onSuccess = { availability ->
                    _uiState.update {
                        it.copy(
                            checking = false,
                            update = availability.metadata,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            checking = false,
                            error = if (it.update?.forceUpdate == true) {
                                error.message ?: "Could not check for updates."
                            } else {
                                null
                            }
                        )
                    }
                }
            )
        }
    }
}
