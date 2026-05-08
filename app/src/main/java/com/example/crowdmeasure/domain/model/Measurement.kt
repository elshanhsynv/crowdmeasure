package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Measurement(
    val meta: Meta,
    val environment: EnvironmentInfo,
    val performance: PerformanceInfo,
//    val config: TestConfig,
//    val phases: List<TestPhase>,
//    val summary: SummaryMetrics,
)