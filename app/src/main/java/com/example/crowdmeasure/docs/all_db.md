// Measurement.kt 

@Serializable
data class Measurement(
val header: SnapshotHeader,
val context: ContextInfo,
val cell: CellInfo? = null,
val wifi: WifiInfo? = null,
val ip: IpInfo? = null,
val performance: PerformanceInfo,
val diagnostics: DiagnosticsInfo? = null,
)

// SnapshotHeader.kt

@Serializable
data class SnapshotHeader(
val timestampUtcMs: Long,
val measurementId: String,
val appVersion: String,
val androidVersion: String,
val androidSdk: Int,
val deviceModel: String,
)

// ContextInfo.kt

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

// WifiInfo.kt

@Serializable
data class WifiInfo(
val rssi: Int? = null,
val linkSpeedMbps: Int? = null,
val txLinkSpeedMbps: Int? = null,
val rxLinkSpeedMbps: Int? = null,
val frequencyMhz: Int? = null,
val channelWidthMhz: Int? = null,
val standard: WifiStandard? = null,
val bssidHash: String? = null,
)

// CellInfo.kt

@Serializable
data class CellInfo(
val carrierName: String? = null,
val mcc: String? = null,
val mnc: String? = null,
val dataNetworkType: String? = null,
val voiceNetworkType: String? = null,
val roaming: Boolean? = null,
val registeredRat: String? = null,
val nrState: NrState = NrState.NONE,
val servingCell: ServingCell? = null,
val signal: SignalInfo? = null,
val radioMetrics: RadioMetrics? = null,
val aggregation: CarrierAggregationInfo? = null,
val availability: AvailabilityFlags = AvailabilityFlags(),
)


@Serializable
data class ServingCell(
val ci: Int? = null,
val nci: Long? = null,
val tac: Int? = null,
val pci: Int? = null,
val earfcn: Int? = null,
val nrarfcn: Long? = null,
val band: Int? = null,
val bandwidthMhz: Int? = null,
)

@Serializable
data class SignalInfo(
val rsrp: Int? = null,
val rsrq: Int? = null,
val sinr: Int? = null,
val rssi: Int? = null,
val cqi: Int? = null,
val timingAdvance: Int? = null,
)

@Serializable
data class AvailabilityFlags(
val cellInfoAccessible: Boolean = false,
val signalAccessible: Boolean = false,
val idsAccessible: Boolean = false,
)

@Serializable
data class CarrierAggregationInfo(
val active: Boolean? = null,
val secondaryCells: List<SecondaryCell> = emptyList(),
)

@Serializable
data class SecondaryCell(
val band: Int? = null,
val earfcn: Int? = null,
val nrarfcn: Long? = null,
val pci: Int? = null,
val rsrp: Int? = null,
val rsrq: Int? = null,
val sinr: Int? = null,
val bandwidthMhz: Int? = null,
)

@Serializable
data class RadioMetrics(
val mimoLayers: Int? = null,
val lteCqi: Int? = null,
val nrCqi: Int? = null,
)

// PerformanceInfo.kt

@Serializable
data class PerformanceInfo(
val endpointId: String,
val dnsMs: Long? = null,
val tcpMs: Long? = null,
val tlsMs: Long? = null,
val ttfbMs: Long? = null,
val rttAvgMs: Long? = null,
val rttP95Ms: Long? = null,
val jitterMs: Long? = null,
val packetLossPct: Double? = null,
val downMbps: Double? = null,
val upMbps: Double? = null,
val downP95Mbps: Double? = null,
val downStdDevMbps: Double? = null,
val upP95Mbps: Double? = null,
val upStdDevMbps: Double? = null,
val stallsCount: Int? = null,
val maxStallMs: Long? = null,
val httpStatus: Int? = null,
val serverRegion: String? = null,
val testPayloadBytes: Long? = null,
val protocol: ProtocolType = ProtocolType.UNKNOWN
)

// IpInfo.kt

@Serializable
data class IpInfo(
val publicIp: String? = null,
val ispName: String? = null,
val asn: Int? = null,
)

// Enums.kt

@Serializable
enum class TransportType { WIFI, CELL, OTHER, NONE }

@Serializable
enum class RecordState { PENDING, UPLOADED, FAILED }

@Serializable
enum class ProtocolType { HTTP1_1, HTTP2, UNKNOWN }

@Serializable
enum class WifiStandard {
UNKNOWN,
WIFI_4,   // 802.11n
WIFI_5,   // 802.11ac
WIFI_6,   // 802.11ax
WIFI_6E,  // Wi-Fi 6 / 7 on 6 GHz
WIFI_7,   // 802.11be
}

@Serializable
enum class NrState {
@SerialName("none") NONE,
@SerialName("nsa")  NSA,
@SerialName("sa")   SA,
}
