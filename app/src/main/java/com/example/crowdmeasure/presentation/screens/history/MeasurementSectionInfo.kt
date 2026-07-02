package com.example.crowdmeasure.presentation.screens.history

import androidx.compose.runtime.Immutable

internal enum class MeasurementSection {
    Summary,
    Device,
    NetworkContext,
    Wifi,
    Cellular,
    Ip,
    Performance
}

@Immutable
internal data class MeasurementSectionContent(
    val section: MeasurementSection,
    val title: String,
    val meaning: String,
    val purpose: String,
    val sensitiveValues: List<SensitiveValueInfo>,
    val metrics: List<MetricDefinition>,
    val shortLabel: String? = null
) {
    fun matches(query: String): Boolean {
        val normalized = query.trim()
        return normalized.isEmpty() ||
                title.contains(normalized, ignoreCase = true) ||
                shortLabel?.contains(normalized, ignoreCase = true) == true ||
                sensitiveValues.any { it.matches(normalized) } ||
                metrics.any { it.matches(normalized) }
    }
}

@Immutable
internal data class SensitiveValueInfo(
    val id: String,
    val name: String,
    val explanation: String,
    val shortLabel: String? = null
) {
    fun matches(query: String) =
        name.contains(query, ignoreCase = true) ||
                shortLabel?.contains(query, ignoreCase = true) == true ||
                explanation.contains(query, ignoreCase = true)
}

@Immutable
internal data class MetricDefinition(
    val id: String,
    val name: String,
    val meaning: String,
    val shortLabel: String? = null,
    val searchTerms: List<String> = emptyList()
) {
    fun matches(query: String) =
        name.contains(query, ignoreCase = true) ||
                shortLabel?.contains(query, ignoreCase = true) == true ||
                meaning.contains(query, ignoreCase = true) ||
                searchTerms.any { it.contains(query, ignoreCase = true) }
}

internal object MeasurementSectionCatalog {
    val all: List<MeasurementSectionContent> = listOf(
        content(
            section = MeasurementSection.Summary,
            title = "Measurement Summary",
            shortLabel = "Summary",
            meaning = "Identifies when this network snapshot was recorded and provides a short reference for it.",
            purpose = "Helps distinguish measurements and place results in time when comparing network conditions.",
            sensitiveValues = listOf(
                sensitive(
                    id = "measurement_id",
                    name = "Measurement ID",
                    shortLabel = "Internal ID",
                    explanation = "An internal identifier. Only a shortened form is shown here."
                )
            ),
            metrics = definitions(
                "Recorded time" to "The local date and time when the measurement was captured.",
                "Measurement ID" to "A unique internal reference used to find this measurement.",
                "Network icon" to "Indicates whether the measurement used Wi-Fi or a cellular connection."
            )
        ),
        content(
            section = MeasurementSection.Device,
            title = "Device & App",
            shortLabel = "Device",
            meaning = "Describes the device and app environment used to capture the measurement.",
            purpose = "Device hardware, Android versions, and app versions can affect signal reporting and test performance. This context makes comparisons fairer.",
            sensitiveValues = listOf(
                sensitive(
                    "session_id",
                    "Session ID",
                    "Groups related app activity without displaying a direct identity.",
                    "Pseudonymous"
                ),
                sensitive(
                    "user_id",
                    "Hashed user ID",
                    "Associates contributions without displaying a raw account identifier.",
                    "Pseudonymous"
                ),
                sensitive(
                    "device_details",
                    "Device and build details",
                    "May contribute to device fingerprinting when combined.",
                    "Linkable"
                )
            ),
            metrics = definitions(
                "Device Model" to "The commercial model of the phone or tablet.",
                "Brand" to "The device brand.",
                "Device Manufacturer" to "The company that manufactured the device.",
                "Hardware" to "Android's hardware platform identifier.",
                "Chipset" to "The device's main processor or system-on-chip.",
                "Chipset Manufacturer" to "The company that produced the chipset.",
                "OS Version" to "The installed Android release.",
                "Android SDK" to "Android's numeric platform API version.",
                "Device OS" to "The operating-system name reported by the device.",
                "Build ID" to "The identifier for the installed Android software build.",
                "App Version" to "The CrowdMeasure version that captured the result.",
                "Session ID" to "A pseudonymous identifier grouping related app activity.",
            )
        ),
        content(
            section = MeasurementSection.NetworkContext,
            title = "Network Context",
            shortLabel = "Context",
            meaning = "Describes the connection and device conditions present during the test.",
            purpose = "Power-saving modes, connection type, data-saving settings, and location can explain differences in availability and performance.",
            sensitiveValues = listOf(
                sensitive(
                    "location",
                    "Precise location",
                    "Capture coordinates are sensitive and hidden by default.",
                    "Hidden"
                ),
                sensitive(
                    "location_accuracy",
                    "Location accuracy",
                    "Describes the possible radius around the captured location.",
                    "Context"
                )
            ),
            metrics = definitions(
                "Transport" to "The active connection type, such as Wi-Fi or cellular.",
                "Internet" to "Whether Android verified that the network could reach the internet.",
                "Captive Portal" to "Whether the connection may require a sign-in page.",
                "VPN" to "Whether traffic was routed through a virtual private network.",
                "Metered" to "Whether Android treats the connection as having limited or chargeable data.",
                "DL Rate" to "The device's observed incoming data rate near capture time.",
                "UL Rate" to "The device's observed outgoing data rate near capture time.",
                "Battery" to "The battery percentage during capture.",
                "Charging" to "Whether the device was connected to power.",
                "Battery Saver" to "Whether Android battery-saving mode was active.",
                "Screen On" to "Whether the screen was on during capture.",
                "Doze Mode" to "Whether Android's idle power-saving mode was active.",
                "Data Saver" to "Whether Android's data-saving mode was active.",
                "Thermal State" to "The device's reported heat or thermal-throttling state.",
                "Memory Usage" to "The percentage of memory in use during capture.",
                "Location" to "The capture coordinates and their estimated accuracy radius.",
            )
        ),
        content(
            section = MeasurementSection.Wifi,
            title = "Wi-Fi",
            meaning = "Describes the local wireless connection used for the measurement.",
            purpose = "Signal quality, radio frequency, and negotiated link rates help explain Wi-Fi performance and coverage.",
            sensitiveValues = listOf(
                sensitive(
                    "bssid_hash",
                    "BSSID hash",
                    "A pseudonymous access-point identifier that can correlate measurements from the same network.",
                    "Pseudonymous"
                )
            ),
            metrics = definitions(
                "Signal Strength (RSSI)" to "Received Wi-Fi signal power in dBm. Values closer to zero generally indicate a stronger signal.",
                "Link Speed (legacy)" to "The older Android estimate of the negotiated Wi-Fi link speed.",
                "TX Link Speed" to "The negotiated link speed for data sent from the device.",
                "RX Link Speed" to "The negotiated link speed for data received by the device.",
                "Frequency" to "The Wi-Fi radio frequency in MHz.",
                "Channel Width" to "The width of the Wi-Fi radio channel in MHz.",
                "Wi-Fi Standard" to "The Wi-Fi generation reported by Android.",
                "SSID" to "The Wi-Fi access point name.",
                "BSSID Hash" to "A hashed identifier representing the connected Wi-Fi access point."
            )
        ),
        content(
            section = MeasurementSection.Cellular,
            title = "Cellular Network",
            shortLabel = "Cellular",
            meaning = "Describes the mobile network, SIM configuration, serving cell, and radio signal conditions.",
            purpose = "These values help map carrier coverage, compare radio technologies, and identify areas with weak or unstable mobile service.",
            sensitiveValues = listOf(
                sensitive(
                    "cell_identifiers",
                    "Cell identifiers",
                    "Serving-cell identity and radio details are hidden by default.",
                    "Hidden"
                ),
                sensitive(
                    "subscription_id",
                    "Subscription ID",
                    "An Android identifier that can link measurements from the same subscription.",
                    "Linkable"
                ),
                sensitive(
                    "card_id",
                    "Card ID",
                    "An Android identifier associated with the SIM card.",
                    "Sensitive"
                ),
                sensitive(
                    "sim_slot",
                    "SIM slot details",
                    "Describe which physical or logical SIM supplied the measurement.",
                    "Linkable"
                )
            ),
            metrics = definitions(
                "RAT" to "The radio access technology, such as LTE or 5G NR.",
                "Data Network Type" to "The mobile technology currently used for data.",
                "Voice Network Type" to "The mobile technology currently used for voice.",
                "Roaming" to "Whether the device was using a network outside its home carrier.",
                "Neighbor Cells" to "The number of nearby cells reported by Android.",
                "Carrier" to "The carrier name associated with a SIM.",
                "Display Name" to "The user-facing subscription name reported by Android.",
                "SIM Operator" to "The operator name stored or reported for a SIM.",
                "SIM Operator ID" to "The numeric mobile country and network code reported for a SIM.",
                "MCC" to "The mobile country code identifying the operator's country.",
                "MNC" to "The mobile network code identifying the operator within a country.",
                "Country" to "The SIM operator's reported country.",
                "Duplex Mode" to "How uplink and downlink radio resources are separated.",
                "Slot Index" to "The physical or logical SIM slot number.",
                "Subscription ID" to "Android's identifier for a mobile subscription.",
                "Carrier ID" to "Android's normalized identifier for a carrier.",
                "Card ID" to "Android's identifier for the SIM card.",
                "Port Index" to "The logical port used by an eSIM profile.",
                "Data Roaming" to "Whether mobile data roaming is enabled for the subscription.",
                "eSIM" to "Whether the subscription is embedded rather than a physical SIM.",
                "Opportunistic" to "Whether the subscription is used opportunistically alongside another subscription.",
                "Active Data" to "Whether this subscription currently carries mobile data.",
                "Default Data" to "Whether this is the preferred subscription for mobile data.",
                "Default Voice" to "Whether this is the preferred subscription for calls.",
                "Default SMS" to "Whether this is the preferred subscription for messages.",
                "Collected Here" to "Marks the SIM from which cellular radio values were collected.",
                "Cell Identifiers" to "A hidden-by-default group containing serving-cell identity, channel, signal, bandwidth, and antenna details."
            )
        ),
        content(
            section = MeasurementSection.Ip,
            title = "IP Information",
            shortLabel = "IP",
            meaning = "Describes the public internet identity and provider visible during the measurement.",
            purpose = "It helps group results by internet provider and understand which network carried the test traffic.",
            sensitiveValues = listOf(
                sensitive(
                    "public_ip",
                    "Public IP",
                    "Can reveal an approximate location or link activity from the same connection.",
                    "Sensitive"
                )
            ),
            metrics = definitions(
                "Public IP" to "The internet-facing IP address observed for this connection.",
                "ISP" to "The internet service provider associated with the public IP.",
                "ASN" to "The autonomous system number identifying the network that announces the public IP."
            )
        ),
        content(
            section = MeasurementSection.Performance,
            title = "Performance",
            meaning = "Measures how quickly and reliably the network reaches a test service and transfers data.",
            purpose = "These metrics show the user-visible quality of a connection and support comparisons across locations, providers, and network types.",
            sensitiveValues = listOf(
                sensitive(
                    "endpoint",
                    "Endpoint identifier",
                    "Hidden by default because it can reveal internal test-routing details.",
                    "Hidden"
                )
            ),
            metrics = definitions(
                "Endpoint" to "The test service selected for this measurement.",
                "Protocol" to "The network protocol used for the performance test.",
                "HTTP Status" to "The response code returned by the test service.",
                "Server Region" to "The broad region hosting the selected test service.",
                "DNS" to "Time needed to resolve a service name to an IP address.",
                "Connection" to "Time needed to establish the network connection.",
                "TLS" to "Time needed to establish an encrypted connection.",
                "TTFB Average" to "Average time until the first response byte arrives.",
                "HTTP Latency Average" to "Average round-trip time for HTTP probes.",
                "HTTP Latency P95" to "A high-end latency value; about 95% of probes were this fast or faster.",
                "Jitter" to "Variation between latency measurements. Lower values indicate more consistent response times.",
                "Probe Failure %" to "Percentage of test probes that received no successful response.",
                "Stalls" to "Number of periods where data transfer stopped unexpectedly.",
                "Max Stall" to "Duration of the longest transfer interruption.",
                "Down" to "Measured download throughput.",
                "Up" to "Measured upload throughput.",
                "Down P95" to "A high-end observed download throughput value.",
                "Up P95" to "A high-end observed upload throughput value.",
                "Down StdDev" to "How much download throughput varied during the test.",
                "Up StdDev" to "How much upload throughput varied during the test."
            )
        )
    )

    private val bySection = all.associateBy(MeasurementSectionContent::section)

    init {
        check(bySection.size == MeasurementSection.entries.size) {
            "Every measurement section must have exactly one information entry"
        }
        all.forEach { content ->
            check(content.metrics.distinctBy(MetricDefinition::id).size == content.metrics.size) {
                "Metric IDs must be unique within ${content.section}"
            }
            check(content.sensitiveValues.distinctBy(SensitiveValueInfo::id).size == content.sensitiveValues.size) {
                "Sensitive value IDs must be unique within ${content.section}"
            }
        }
    }

    operator fun get(section: MeasurementSection): MeasurementSectionContent =
        checkNotNull(bySection[section]) { "Missing information for $section" }

    fun search(query: String): List<MeasurementSectionContent> =
        all.filter { it.matches(query) }
}

private fun content(
    section: MeasurementSection,
    title: String,
    meaning: String,
    purpose: String,
    sensitiveValues: List<SensitiveValueInfo>,
    metrics: List<MetricDefinition>,
    shortLabel: String? = null
) = MeasurementSectionContent(
    section = section,
    title = title,
    shortLabel = shortLabel,
    meaning = meaning,
    purpose = purpose,
    sensitiveValues = sensitiveValues,
    metrics = metrics
)

private fun sensitive(
    id: String,
    name: String,
    explanation: String,
    shortLabel: String? = null
) = SensitiveValueInfo(
    id = id,
    name = name,
    explanation = explanation,
    shortLabel = shortLabel
)

private fun definitions(vararg entries: Pair<String, String>) =
    entries.map { (name, meaning) ->
        MetricDefinition(
            id = name.toStableId(),
            name = name,
            meaning = meaning
        )
    }

private fun String.toStableId() =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
