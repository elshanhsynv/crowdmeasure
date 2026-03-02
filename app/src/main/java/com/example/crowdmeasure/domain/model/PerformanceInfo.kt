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
    val downP95Mbps: Double? = null,
    val downStdDevMbps: Double? = null,
    val upP95Mbps: Double? = null,
    val upStdDevMbps: Double? = null,
    val stallsCount: Int? = null,
    val maxStallMs: Long? = null,
    val httpStatus: Int? = null,
    val serverRegion: String? = null,
    val testPayloadBytes: Long? = null,
    val protocol: ProtocolType = ProtocolType.UNKNOWN
)
