package com.crowdmeasure.sdk.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cellular context for one collection
 */
@Serializable
data class CellInfo(
    val simCarriers: List<CarrierInfo> = emptyList(),
    val collectedSubscriptionId: Int? = null,
    val collectedSimSlotIndex: Int? = null,
    val dataNetworkType: String? = null,
    val voiceNetworkType: String? = null,
    val roaming: Boolean? = null,
    val rat: String?,
    val nrState: NrState,
    val serving: CellRadioSnapshot?,
    val neighbors: List<CellRadioSnapshot> = emptyList(),
)

@Serializable
data class CarrierInfo(
    val carrierName: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val simOperatorId: String? = null,
    val simOperatorName: String? = null,
    val countryIso: String? = null,
    val duplexMode: String? = null,
    val subscriptionId: Int? = null,
    val simSlotIndex: Int? = null,
    val displayName: String? = null,
    val carrierId: Int? = null,
    val dataRoaming: Boolean? = null,
    val isEmbedded: Boolean? = null,
    val cardId: Int? = null,
    val isDefaultData: Boolean? = null,
    val isDefaultVoice: Boolean? = null,
    val isDefaultSms: Boolean? = null,
    val isActiveData: Boolean? = null,
)

@Serializable
data class CellRadioSnapshot(
    // Cell observation time minus measurement start, both from elapsed realtime.
    val timestampOffsetMs: Long?,
    val radio: CellRadio,
    // Android's technology-specific display value, not an independent metric.
    val dbm: Int? = null,
    val asuLevel: Int? = null,
    // Android's 0..4 quality category; zero means none/unknown, not missing.
    val level: Int? = null,
    val subscriptionId: Int? = null,
    val isRegistered: Boolean? = null,
)

@Serializable
sealed interface CellRadio {
    @Serializable
    @SerialName("gsm")
    data class Gsm(
        val cellId: Int? = null,
        val lac: Int? = null,
        val bsic: Int? = null,
        val arfcn: Int? = null,
        val rssiDbm: Int? = null,
        // Raw GSM timing advance, in symbol periods; not the LTE unit.
        val timingAdvance: Int? = null,
    ) : CellRadio

    @Serializable
    @SerialName("wcdma")
    data class Wcdma(
        val cellId: Int? = null,
        val lac: Int? = null,
        val psc: Int? = null,
        val uarfcn: Int? = null,
        val ecNoDb: Int? = null,
    ) : CellRadio

    @Serializable
    @SerialName("lte")
    data class Lte(
        val cellId: Int? = null,
        val tac: Int? = null,
        val pci: Int? = null,
        val earfcn: Int? = null,
        // Empty means unreported, not "no supported bands".
        val bands: List<Int> = emptyList(),
        val bandwidthKhz: Int? = null,
        val rsrpDbm: Int? = null,
        val rsrqDb: Int? = null,
        val rssnrDb: Int? = null,
        val rssiDbm: Int? = null,
        val cqi: Int? = null,
        // Raw Android LTE timing-advance value. Do not treat as meters.
        val timingAdvance: Int? = null,
        val mimoLayers: Int? = null,
    ) : CellRadio

    @Serializable
    @SerialName("nr")
    data class Nr(
        val cellId: Long? = null,
        val tac: Int? = null,
        val pci: Int? = null,
        val nrarfcn: Int? = null,
        val bands: List<Int> = emptyList(),
        val ss: NrSignal? = null,
        val csi: NrSignal? = null,
        val bandwidthKhz: Int? = null,
        val mimoLayers: Int? = null,
    ) : CellRadio
}

@Serializable
data class NrSignal(
    val rsrpDbm: Int? = null,
    val rsrqDb: Int? = null,
    val sinrDb: Int? = null,
)

