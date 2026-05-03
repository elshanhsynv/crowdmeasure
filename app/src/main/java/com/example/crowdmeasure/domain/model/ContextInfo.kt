package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ContextInfo(
    val location: Location? = null,
    val transport: TransportType,
    val validatedInternet: Boolean? = null,
    val captivePortal: Boolean? = null,
    val metered: Boolean? = null,
    val vpnPresent: Boolean? = null,
    val batterySaver: Boolean,
    val batteryPercentage: Int? = null,
    val charging: Boolean,
    val screenOn: Boolean,
)

@Serializable
data class Location(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
)