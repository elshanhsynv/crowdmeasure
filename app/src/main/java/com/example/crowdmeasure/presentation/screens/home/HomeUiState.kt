package com.example.crowdmeasure.presentation.screens.home

import androidx.compose.runtime.Immutable
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.EnvironmentInfo
import com.crowdmeasure.sdk.model.Meta
import com.crowdmeasure.sdk.model.PerformanceInfo
import com.crowdmeasure.sdk.model.WifiInfo
import com.example.crowdmeasure.presentation.util.UiState

@Immutable
data class HomeUiState(
    val uploadsEnabled: Boolean = false,

    // Derived permission state
    val canCollect: Boolean = false,
    val locationServicesOn: Boolean = true,

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
    val meta: Meta,
    val environment: EnvironmentInfo,
    val cell: CellInfo? = null,
    val wifi: WifiInfo? = null,
    val performance: PerformanceInfo,
    val feedbackTag: String? = null
)
