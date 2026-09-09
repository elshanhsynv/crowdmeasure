package com.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class Meta(
    val measurementId: String,
    val timestampUtcMs: Long,

    // Device
    /** Human-readable app name, e.g. "CrowdMeasure". */
    val appName: String,
    /** Human-readable app version, e.g. "1.0.0". */
    val appVersion: String,
    /** Human-readable Android release string, e.g. "15". */
    val androidRelease: String,
    /** Numeric API level, e.g. 35. */
    val androidSdk: Int,
    /** "<MANUFACTURER> <MODEL>" trimmed, e.g. "Google Pixel 9". */
    val deviceModel: String,
    val brand: String,
    val deviceManufacturer: String,
    val deviceOS: String,
    val buildID: String,
    val hardware: String,
    val chipset: String,
    val chipsetManufacturer: String,
    val userPhoneNumber: String?,
)