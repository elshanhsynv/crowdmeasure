package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TestPhase(
    val type: PhaseType,
    val metrics: PhaseMetrics,
    val series: PhaseSeries?,
)

@Serializable
data class PhaseMetrics(
    val durationMs: Long,

    val throughputAvgMbps: Double?,
    val throughputP95Mbps: Double?,
    val throughputStdDev: Double?,

    val latencyAvgMs: Double?,
    val latencyP95Ms: Double?,
    val jitterMs: Double?,

    val packetLossPct: Double?,
)

@Serializable
data class PhaseSeries(
    val throughput: List<ThroughputSample>?,
    val latency: List<LatencySample>?,
)

@Serializable
data class ThroughputSample(
    val tMs: Long,
    val mbps: Double,
)

@Serializable
data class LatencySample(
    val tMs: Long,
    val rttMs: Double,
)