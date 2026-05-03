package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Measurement(
    val header: SnapshotHeader,
    val context: ContextInfo,
    val cell: CellInfo? = null,
    val wifi: WifiInfo? = null,
    val ip: IpInfo? = null,
    val performance: PerformanceInfo,
    val diagnostics: DiagnosticsInfo? = null,
)
