package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ContextInfo(
    val coarseLocation: CoarseLocation? = null,
    val transport: TransportType,
    val validatedInternet: Boolean? = null,
    val captivePortal: Boolean? = null,
    val metered: Boolean? = null,
    val vpnPresent: Boolean? = null,
    val batterySaver: Boolean,
    val charging: Boolean,
    val screenOn: Boolean,
    val foreground: Boolean
)

@Serializable
data class CoarseLocation(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float
)