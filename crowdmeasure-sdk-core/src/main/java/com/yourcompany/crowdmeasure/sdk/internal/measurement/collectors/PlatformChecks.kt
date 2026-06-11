package com.crowdmeasure.sdk.internal.measurement.collectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

internal object PlatformChecks {
    fun hasCoarseLocation(context: Context) =
        hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun hasFineLocation(context: Context) =
        hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasPhoneState(context: Context) =
        hasPermission(context, Manifest.permission.READ_PHONE_STATE)

    fun isLocationServicesEnabled(context: Context) =
        runCatching {
            context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true
        }.getOrDefault(false)

    private fun hasPermission(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
