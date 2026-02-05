package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RadioMetrics(
    val mimoLayers: Int? = null,
    val lteCqi: Int? = null,
    val nrCqi: Int? = null
)
