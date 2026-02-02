package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CellInfo(
    val carrierName: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val dataNetworkType: String? = null,
    val voiceNetworkType: String? = null,
    val roaming: Boolean? = null,
    val registeredRat: String? = null,
    val servingCell: ServingCell? = null,
    val signal: SignalInfo? = null,
    val availability: AvailabilityFlags = AvailabilityFlags()
)

@Serializable
data class ServingCell(
    val ci: Int? = null,
    val nci: Long? = null,
    val tac: Int? = null,
    val pci: Int? = null,
    val earfcn: Int? = null,
    val nrarfcn: Int? = null,
    val band: Int? = null
)

@Serializable
data class SignalInfo(
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val rssi: Int? = null
)

@Serializable
data class AvailabilityFlags(
    val cellInfoAccessible: Boolean = false,
    val signalAccessible: Boolean = false,
    val idsAccessible: Boolean = false
)