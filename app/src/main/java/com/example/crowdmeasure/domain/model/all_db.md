// Measurement.kt 

@Serializable
data class Measurement(
val header: SnapshotHeader,
val context: ContextInfo,
val cell: CellInfo? = null,
val wifi: WifiInfo? = null,
val performance: PerformanceInfo,
val feedbackTag: String? = null
)

// SnapshotHeader.kt

@Serializable
data class SnapshotHeader(
val timestampUtcMs: Long,
val measurementId: String,
val appVersion: String,
val androidVersion: String,
val deviceModel: String,
val userConsentVersion: Int
)

// ContextInfo.kt

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

// WifiInfo.kt

@Serializable
data class WifiInfo(
val rssi: Int? = null,
val linkSpeedMbps: Int? = null,
val frequencyMhz: Int? = null,
val channelWidthMhz: Int? = null
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
val servingCell: ServingCell? = null,
val signal: SignalInfo? = null,
val availability: AvailabilityFlags = AvailabilityFlags()
)

@Serializable
data class ServingCell(
val ci: Int? = null,
val nci: Long? = null,
val tac: Int? = null,
val pci: Int? = null,
val earfcn: Int? = null,
val nrarfcn: Int? = null,
val band: Int? = null
)

@Serializable
data class SignalInfo(
val rsrp: Int? = null,
val rsrq: Int? = null,
val sinr: Int? = null,
val rssi: Int? = null
)

@Serializable
data class AvailabilityFlags(
val cellInfoAccessible: Boolean = false,
val signalAccessible: Boolean = false,
val idsAccessible: Boolean = false
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
val testPayloadBytes: Long? = null,
val protocol: ProtocolType = ProtocolType.UNKNOWN
)