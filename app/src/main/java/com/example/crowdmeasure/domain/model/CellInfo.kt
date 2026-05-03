package com.example.crowdmeasure.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 5G NR operating mode.
 *
 * [NONE] — no NR component detected.
 * [NSA] — Non-Standalone: NR secondary cell anchored to LTE.
 * [SA]  — Standalone: NR serving cell with no LTE anchor.
 */
@Serializable
enum class NrState {
    @SerialName("none") NONE,
    @SerialName("nsa")  NSA,
    @SerialName("sa")   SA,
}

/**
 * Telephony snapshot for a single measurement.
 *
 * [nrState] is always set; [NrState.NONE] means no NR component was detected
 * (not that the check was skipped — use [AvailabilityFlags] for that).
 */
@Serializable
data class CellInfo(
    val carrierName: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val dataNetworkType: String? = null,
    val voiceNetworkType: String? = null,
    val roaming: Boolean? = null,
    val registeredRat: String? = null,
    val nrState: NrState = NrState.NONE,   // non-nullable; NONE = "not present", not "unknown"
    val servingCell: ServingCell? = null,
    val signal: SignalInfo? = null,
    val radioMetrics: RadioMetrics? = null,
    val aggregation: CarrierAggregationInfo? = null,
    val availability: AvailabilityFlags = AvailabilityFlags(),
)

@Serializable
data class ServingCell(
    val ci: Int? = null,
    val nci: Long? = null,
    val tac: Int? = null,
    val pci: Int? = null,
    val earfcn: Int? = null,
    val nrarfcn: Long? = null,
    val band: Int? = null,
    val bandwidthMhz: Int? = null,
)

@Serializable
data class SignalInfo(
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val rssi: Int? = null,
    val cqi: Int? = null,
    val timingAdvance: Int? = null,
)

@Serializable
data class AvailabilityFlags(
    val cellInfoAccessible: Boolean = false,
    val signalAccessible: Boolean = false,
    val idsAccessible: Boolean = false,
)

@Serializable
data class CarrierAggregationInfo(
    /**
     * Whether carrier aggregation is actively in use.
     * Null = cannot be determined from public API (the common case).
     * Seeing secondary cells is necessary but not sufficient evidence of active CA.
     */
    val active: Boolean? = null,
    val secondaryCells: List<SecondaryCell> = emptyList(),
)

@Serializable
data class SecondaryCell(
    val band: Int? = null,
    val earfcn: Int? = null,
    val nrarfcn: Long? = null,
    val pci: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val bandwidthMhz: Int? = null,
)

@Serializable
data class RadioMetrics(
    val mimoLayers: Int? = null,
    val lteCqi: Int? = null,
    val nrCqi: Int? = null,
)