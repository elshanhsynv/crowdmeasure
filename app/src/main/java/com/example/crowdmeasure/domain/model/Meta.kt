package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Meta(
    val measurementId: String,
    val timestampUtcMs: Long,

    // Device
    val deviceModel: String,
    val osVersion: String,
    val sdkInt: Int,
    val appVersion: String,

    // Session grouping (very useful for ML)
    val sessionId: String?,  // multiple tests in a row
    val userIdHash: String?, // optional privacy-safe cohorting
)