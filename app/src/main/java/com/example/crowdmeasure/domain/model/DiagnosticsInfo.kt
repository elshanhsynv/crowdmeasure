package com.example.crowdmeasure.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed representation of [android.os.PowerManager] thermal status constants.
 *
 * Maps 1-to-1 to [android.os.PowerManager.THERMAL_STATUS_*] (API 29+).
 * [UNKNOWN] is used when the API is unavailable (< API 29) or returns an
 * unrecognized value.
 */
@Serializable
enum class ThermalStatus {
    @SerialName("none")     NONE,        // THERMAL_STATUS_NONE     = 0
    @SerialName("light")    LIGHT,       // THERMAL_STATUS_LIGHT    = 1
    @SerialName("moderate") MODERATE,    // THERMAL_STATUS_MODERATE = 2
    @SerialName("severe")   SEVERE,      // THERMAL_STATUS_SEVERE   = 3
    @SerialName("critical") CRITICAL,    // THERMAL_STATUS_CRITICAL = 4
    @SerialName("emergency") EMERGENCY,  // THERMAL_STATUS_EMERGENCY= 5
    @SerialName("shutdown") SHUTDOWN,    // THERMAL_STATUS_SHUTDOWN = 6
    @SerialName("unknown")  UNKNOWN,     // < API 29 or unrecognized value
}

/**
 * Supplementary device diagnostics captured at measurement time.
 *
 * All fields are nullable: null means "could not be determined", not "false".
 *
 * Fields for future collection ([handoverCount], [handoverDuringTest],
 * [publicIpHash], [asn]) are intentionally omitted until implemented;
 * they will be absent from payloads rather than serialized as null.
 */
@Serializable
data class DiagnosticsInfo(
    /** Null on API < 29 or when PowerManager is unavailable. */
    val thermalStatus: ThermalStatus? = null,
    /** True when the device is in Doze / idle mode. Available API 23+. */
    val dozeMode: Boolean? = null,
    /**
     * True when Data Saver is globally enabled (even if this app is whitelisted).
     * Null when ConnectivityManager is unavailable or status is unexpected.
     */
    val dataSaverEnabled: Boolean? = null,
)