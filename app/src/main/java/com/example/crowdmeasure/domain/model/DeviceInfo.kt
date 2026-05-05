package com.example.crowdmeasure.domain.model

import android.os.Build
import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfo(
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
    val chipsetManufacturer: String
    )