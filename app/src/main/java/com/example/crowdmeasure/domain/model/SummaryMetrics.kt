package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SummaryMetrics(
    val downlinkMbps: Double,
    val uplinkMbps: Double,

    val latencyIdleMs: Double,
    val latencyDownloadMs: Double,
    val latencyUploadMs: Double,

    val jitterMs: Double,
    val packetLossPct: Double,

    // Stability (very useful)
    val throughputStability: Double, // derived (stddev / mean)

    // QoE proxies
    val estimatedWebLoadMs: Double?,
    val estimatedVideoStartMs: Double?,
    val estimatedMos: Double?, // VoIP quality estimate
)