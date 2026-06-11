package com.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class Measurement(
    val meta: Meta,
    val environment: EnvironmentInfo,
    val performance: PerformanceInfo,
)