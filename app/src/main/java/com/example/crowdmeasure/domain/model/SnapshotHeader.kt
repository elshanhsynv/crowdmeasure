package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SnapshotHeader(
    val timestampUtcMs: Long,
    val measurementId: String,
    val appVersion: String,
    val androidVersion: String,
    val androidSdk: Int,
    val deviceModel: String,
)