package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CellInfo(
    val simCarriers: List<CarrierInfo> = emptyList(),
    val collectedSubscriptionId: Int? = null,
    val collectedSimSlotIndex: Int? = null,
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
    val simOperatorId: String?,   // MCC+MNC combined
    val simOperatorName: String?,
    val countryIso: String?,
    val duplexMode: String?,
    val subscriptionId: Int? = null,
    val simSlotIndex: Int? = null,
    val displayName: String? = null,
    val carrierId: Int? = null,
    val dataRoaming: Boolean? = null,
    val isEmbedded: Boolean? = null,
    val isOpportunistic: Boolean? = null,
    val cardId: Int? = null,
    val portIndex: Int? = null,
    val isDefaultData: Boolean? = null,
    val isDefaultVoice: Boolean? = null,
    val isDefaultSms: Boolean? = null,
    val isActiveData: Boolean? = null,
)

@Serializable
data class CellRadioSnapshot(
    val timestampOffsetMs: Long?,

    // --- Cell Identity (multi-RAT support) ---
    val cellId: Int?,          // LTE CI / fallback
    val cid: Int?,             // 2G/3G explicit
    val nci: Long?,            // 5G NR

    val lac: Int?,             // 2G/3G
    val tac: Int?,             // LTE/NR

    val pci: Int?,
    val psc: Int?,             // 3G
    val bsic: Int?,            // 2G

    val band: Int?,
    val arfcn: Int?,           // LTE
    val uarfcn: Int?,          // 3G
    val nrarfcn: Int?,         // 5G

    // --- Signal (generic) ---
    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val rssiDbm: Int?,
    val cqi: Int?,

    // --- Signal normalization ---
    val asuLevel: Int?,        // Android normalized signal
    val dbm: Int?,             // unified signal strength

    // --- LTE specific ---
    val timingAdvance: Int?,   // IMPORTANT for distance estimation

    // --- 5G NR (SS + CSI separation) ---
    val ssRsrpDbm: Int?,
    val ssRsrqDb: Int?,
    val ssSinrDb: Int?,

    val csiRsrpDbm: Int?,
    val csiRsrqDb: Int?,
    val csiSinrDb: Int?,

    // --- Capacity ---
    val bandwidthMhz: Int?,
    val mimoLayers: Int?,
)

@Serializable
data class SecondaryCell(
    val band: Int? = null,
    val earfcn: Int? = null,
    val nrarfcn: Int? = null,
    val pci: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val asuLevel: Int? = null,
    val dbm: Int? = null,
    val bandwidthMhz: Int? = null,
)

@Serializable
data class CarrierAggregationInfo(
    val active: Boolean? = null,
    val secondaryCells: List<SecondaryCell> = emptyList(),
)
