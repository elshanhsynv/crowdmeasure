package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EnvironmentInfo(
    val location: Location?,
    val network: NetworkEnvironment,
    val device: DeviceEnvironment,
)

@Serializable
data class Location(
    val lat: Double,
    val lon: Double,
    val accuracyMeters: Float,
)

@Serializable
data class NetworkEnvironment(
    val transport: TransportType,

    // IP layer
    val ip: IpInfo,

    // Connectivity flags
    val validatedInternet: Boolean?,
    val captivePortal: Boolean?,
    val vpn: Boolean?,
    val metered: Boolean?,

    // WiFi / Cellular
    val wifi: WifiInfo?,
    val cell: CellInfo?,
)

@Serializable
data class DeviceEnvironment(
    val batteryPct: Int?,
    val charging: Boolean,
    val batterySaver: Boolean?,
    val screenOn: Boolean,
    val dozeMode: Boolean?,
    val dataSaver: Boolean?,
    val thermalState: String?,   // NORMAL / HOT / THROTTLED
    val cpuUsagePct: Double?,   // coarse estimate
    val memoryUsagePct: Double?,
)