package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WifiInfo(
    val rssi: Int? = null,
    val linkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    val channelWidthMhz: Int? = null
)