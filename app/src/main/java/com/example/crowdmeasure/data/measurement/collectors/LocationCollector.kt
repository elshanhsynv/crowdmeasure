package com.example.crowdmeasure.data.measurement.collectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.crowdmeasure.domain.model.CoarseLocation
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationCollector {

    suspend fun tryGetCoarseOneShot(context: Context): CoarseLocation? {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) return null

        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 0L)
            .setMaxUpdates(1)
            .build()

        return suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc == null) cont.resume(null)
                    else cont.resume(CoarseLocation(loc.latitude, loc.longitude, loc.accuracy))
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }
}