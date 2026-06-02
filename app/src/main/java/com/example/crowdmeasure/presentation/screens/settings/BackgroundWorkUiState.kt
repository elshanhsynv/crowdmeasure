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
    val nextScheduledWorkStateLabel: String,
    val intervalMinutesLabel: String,
    val lastStartLabel: String,
    val lastEndLabel: String,
    val lastResultLabel: String,
    val autoRunLastCodeLabel: String,
    val autoRunLastSuccessfulCollectionLabel: String,
    val autoRunLastMeasurementLabel: String,
    val uploadLastSuccessfulUploadLabel: String,
    val uploadLastStartLabel: String,
    val uploadLastEndLabel: String,
    val uploadLastResultLabel: String,
    val uploadLastCodeLabel: String,
    val lastUploadedLabel: String,
    val pendingRecordsLabel: String,
    val failedRecordsLabel: String,
    val lastErrorLabel: String,
    val canRunNow: Boolean,
    val canReschedule: Boolean
) {
    companion object {
        fun loading() = BackgroundWorkUiState(
            workManagerStateLabel = "Loading...",
            nextScheduledWorkStateLabel = "Loading...",
            intervalMinutesLabel = "—",
            lastStartLabel = "—",
            lastEndLabel = "—",
            lastResultLabel = "—",
            autoRunLastCodeLabel = "—",
            autoRunLastSuccessfulCollectionLabel = "—",
            autoRunLastMeasurementLabel = "—",
            uploadLastSuccessfulUploadLabel = "—",
            uploadLastStartLabel = "—",
            uploadLastEndLabel = "—",
            uploadLastResultLabel = "—",
            uploadLastCodeLabel = "—",
            lastUploadedLabel = "—",
            pendingRecordsLabel = "—",
            failedRecordsLabel = "—",
            lastErrorLabel = "None",
            canRunNow = false,
            canReschedule = false
        )
    }
}
