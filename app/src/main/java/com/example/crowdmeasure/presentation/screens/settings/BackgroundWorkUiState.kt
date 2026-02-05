package com.example.crowdmeasure.presentation.screens.settings

import androidx.compose.runtime.Immutable

/**
 * UI state for background work status section.
 *
 * Shows WorkManager state + last run details.
 * All values pre-formatted for display.
 */
@Immutable
data class BackgroundWorkUiState(
    val workManagerStateLabel: String,
    val intervalMinutesLabel: String,
    val lastStartLabel: String,
    val lastEndLabel: String,
    val lastResultLabel: String,
    val lastUploadedLabel: String,
    val lastErrorLabel: String,
    val canRunNow: Boolean,
    val canReschedule: Boolean
) {
    companion object {
        fun loading() = BackgroundWorkUiState(
            workManagerStateLabel = "Loading...",
            intervalMinutesLabel = "—",
            lastStartLabel = "—",
            lastEndLabel = "—",
            lastResultLabel = "—",
            lastUploadedLabel = "—",
            lastErrorLabel = "None",
            canRunNow = false,
            canReschedule = false
        )
    }
}