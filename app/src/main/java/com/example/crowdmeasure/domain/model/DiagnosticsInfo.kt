package com.example.crowdmeasure.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable



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