package com.example.crowdmeasure.presentation.screens.home

import androidx.compose.runtime.Immutable
import com.example.crowdmeasure.domain.model.CellInfo
import com.example.crowdmeasure.domain.model.PerformanceInfo
import com.example.crowdmeasure.domain.model.WifiInfo
import com.example.crowdmeasure.presentation.util.UiState

@Immutable
data class HomeUiState(
    val uploadsEnabled: Boolean = false,

    // Derived permission state
    val canCollect: Boolean = false,

    // Data state
    val queuedCount: Int = 0,
    val lastMeasurement: MeasurementUi? = null,

    // Operation state (renamed for clarity)
    val measurementState: UiState<Unit> = UiState.Idle,
    val uploadState: UiState<Int> = UiState.Idle,
) {
    /**
     * Whether a measurement is currently running.
     */
    val isMeasuring: Boolean
        get() = measurementState is UiState.Loading

    /**
     * Whether an upload is currently in progress.
     */
    val isUploading: Boolean
        get() = uploadState is UiState.Loading

    /**
     * Whether the "Upload now" button should be enabled.
     */
    val canUpload: Boolean
        get() = canCollect && uploadsEnabled && queuedCount > 0 && !isUploading
}

@Immutable
data class MeasurementUi(
    val header: SnapshotHeader,
    val context: ContextInfo,
    val cell: CellInfo? = null,
    val wifi: WifiInfo? = null,
    val performance: PerformanceInfo,
    val feedbackTag: String? = null
)