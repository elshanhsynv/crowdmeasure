package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PerformanceInfo(
    val endpointId: String,
    val dnsMs: Long? = null,
    val tcpMs: Long? = null,
    val tlsMs: Long? = null,
    val ttfbMs: Long? = null,
    val rttAvgMs: Long? = null,
    val rttP95Ms: Long? = null,
    val jitterMs: Long? = null,
    val packetLossPct: Double? = null,
    val downMbps: Double? = null,
    val upMbps: Double? = null,
    val testPayloadBytes: Long? = null,
    val protocol: ProtocolType = ProtocolType.UNKNOWN
)