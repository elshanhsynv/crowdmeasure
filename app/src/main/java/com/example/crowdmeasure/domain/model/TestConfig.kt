package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TestConfig(
    val server: ServerInfo,
    val protocol: ProtocolType,

    val download: LoadConfig,
    val upload: LoadConfig,

    val latencyProbe: LatencyProbeConfig,
)

@Serializable
data class ServerInfo(
    val serverId: String,
    val host: String,
    val ip: String,
//    val distanceKm: Int,
//    val selectionMethod: String,
    val pretestLatencyMs: Long,
)

@Serializable
data class LoadConfig(
    val streams: Int,
    val durationMs: Long,
    val warmupMs: Long?,
    val payloadBytes: Long?,
)

@Serializable
data class LatencyProbeConfig(
    val sampleCount: Int,
    val intervalMs: Long,
    val duringLoad: Boolean,
)