package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CellInfo(
    val carrier: CarrierInfo,
    val rat: String?, // LTE, NR, etc.
    val nrState: NrState,

    val dataNetworkType: String? = null,
    val voiceNetworkType: String? = null,
    val roaming: Boolean? = null,

    val serving: CellRadioSnapshot?,
    val neighbors: List<CellRadioSnapshot> = emptyList(),

    val aggregation: CarrierAggregationInfo?,
)

@Serializable
data class CarrierInfo(
    val carrierName: String?,
    val mcc: String?,
    val mnc: String?,
)

@Serializable
data class CellRadioSnapshot(
    val timestampOffsetMs: Long?,

    val cellId: Int?,
    val nci: Long?,
    val band: Int?,
    val arfcn: Int?,
    val nrarfcn: Int?,
    val tac: Int?,
    val pci: Int?,

    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val cqi: Int?,
    val rssi: Int?,

    val bandwidthMhz: Int?,
    val mimoLayers: Int?,
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
data class CarrierAggregationInfo(
    val active: Boolean? = null,
    val secondaryCells: List<SecondaryCell> = emptyList(),
)
