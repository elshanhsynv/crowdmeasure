package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.runtime.Immutable
import com.example.crowdmeasure.presentation.util.UiState

@Immutable
data class MeasurementDetailUiState(
    val loadState: UiState<MeasurementDetailUi> = UiState.Loading,
    val revealed: Set<RevealKey> = emptySet()
) {
    val measurement: MeasurementDetailUi?
        get() = (loadState as? UiState.Success)?.data
}

@Immutable
data class MeasurementDetailUi(
    val id: String,
    val timeText: String,
    val meta: List<Pair<String, String?>>,
    val env: List<Pair<String, String>>,
    val wifi: List<Pair<String, String>>?,
    val cell: List<Pair<String, String>>?,
    val sims: List<SimCarrierUi> = emptyList(),
    val collectedSimText: String? = null,
    val ip: List<Pair<String, String>>?,
    val performance: PerformanceUi,

    // Sensitive data (masked by default)
    val endpointId: String?,
    val locationText: String?,
    val cellIdsText: String?
)

@Immutable
data class SimCarrierUi(
    val title: String,
    val subtitle: String?,
    val isCollected: Boolean,
    val pairs: List<Pair<String, String>>
)

@Immutable
data class PerformanceUi(
    val protocol: String,
    val dns: String,
    val connect: String,
    val tls: String,
    val ttfbAvg: String,
    val httpLatencyAvg: String,
    val httpLatencyP95: String,
    val jitter: String,
    val probeFailure: String,
    val httpStatus: String,
    val serverRegion: String,
    val stallsCount: String,
    val maxStall: String,
    val down: String,
    val up: String,
    val downP95: String,
    val downStdDev: String,
    val upP95: String,
    val upStdDev: String
)

enum class RevealKey {
    Endpoint,
    Location,
    CellIds
}
