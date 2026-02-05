package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DiagnosticsInfo(
    val thermalStatus: Int? = null,
    val dozeMode: Boolean? = null,
    val dataSaverEnabled: Boolean? = null,
    val handoverCount: Int? = null,
    val handoverDuringTest: Boolean? = null,
    val publicIpHash: String? = null,
    val asn: Int? = null,
    val ispName: String? = null
)
