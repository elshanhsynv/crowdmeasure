package com.example.crowdmeasure.presentation.screens.settings

import androidx.compose.runtime.Immutable

@Immutable
data class CallSamplingStatusUiState(
    val lastMissedLabel: String
) {
    companion object {
        fun empty() = CallSamplingStatusUiState(lastMissedLabel = "None")
    }
}
