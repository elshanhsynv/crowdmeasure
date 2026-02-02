package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SnapshotHeader(
    val timestampUtcMs: Long,
    val measurementId: String,
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
    val userConsentVersion: Int
)