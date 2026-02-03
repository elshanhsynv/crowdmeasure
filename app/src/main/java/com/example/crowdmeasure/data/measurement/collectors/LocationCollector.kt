package com.example.crowdmeasure.data.measurement.collectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.example.crowdmeasure.domain.model.CoarseLocation
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationCollector {

    suspend fun tryGetCoarseOneShot(context: Context): CoarseLocation? {
        if (!hasCoarsePermission(context)) return null
        if (!isLocationServicesEnabled(context)) return null

        // 0) Try Fused (best UX when it works)
        val fused = tryFused(context)
        if (fused != null) return fused

        // 1) Fallback to platform LocationManager (NETWORK_PROVIDER is "coarse-friendly")
        val platform = tryPlatformNetworkProvider(context)
        if (platform != null) return platform

        // 2) Last resort: best-effort single fused update (you had this already)
        val lastResort = tryFusedSingleUpdate(context)
        return lastResort
    }

    private fun hasCoarsePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationServicesEnabled(context: Context): Boolean {
        val lm = context.getSystemService<LocationManager>() ?: return false
        return try {
            lm.isLocationEnabled
        } catch (_: Throwable) {
            false
        }
    }

    private suspend fun tryFused(context: Context): CoarseLocation? {
        val client = LocationServices.getFusedLocationProviderClient(context)

        // 1) current
        val current = awaitCurrentLocation(client)
        if (current != null) return current.toCoarse()

        // 2) last
        val last = awaitLastLocation(client)
        if (last != null) return last.toCoarse()

        return null
    }

    private suspend fun tryPlatformNetworkProvider(context: Context): CoarseLocation? {
        val lm = context.getSystemService<LocationManager>() ?: return null

        // Try last known from NETWORK_PROVIDER first (very common to exist)
        val last = runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }
            .getOrNull()
        if (last != null) return last.toCoarse()

        // If no last known, request a single update with timeout
        return withTimeoutOrNull(6_000L) {
            suspendCancellableCoroutine { cont ->
                var finished = false
                fun finish(loc: Location?) {
                    if (finished) return
                    finished = true
                    cont.resume(loc?.toCoarse())
                }

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        lm.removeUpdates(this)
                        finish(location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                }

                cont.invokeOnCancellation {
                    runCatching { lm.removeUpdates(listener) }
                }

                try {
                    // NETWORK_PROVIDER is the right choice for COARSE.
                    lm.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER,
                        listener,
                        Looper.getMainLooper()
                    )
                } catch (_: SecurityException) {
                    finish(null)
                } catch (_: IllegalArgumentException) {
                    // Provider missing/disabled on some devices
                    finish(null)
                } catch (_: Throwable) {
                    finish(null)
                }
            }
        }
    }

    private suspend fun tryFusedSingleUpdate(context: Context): CoarseLocation? {
        val client = LocationServices.getFusedLocationProviderClient(context)

        return withTimeoutOrNull(6_000L) {
            suspendCancellableCoroutine { cont ->
                val request = LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    2_000L
                ).setMaxUpdates(1).build()

                var finished = false
                fun finish(loc: Location?) {
                    if (finished) return
                    finished = true
                    cont.resume(loc?.toCoarse())
                }

                val callback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        client.removeLocationUpdates(this)
                        finish(result.lastLocation)
                    }
                }

                cont.invokeOnCancellation { client.removeLocationUpdates(callback) }

                client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                    .addOnFailureListener {
                        client.removeLocationUpdates(callback)
                        finish(null)
                    }
            }
        }
    }

    private suspend fun awaitCurrentLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): Location? = suspendCancellableCoroutine { cont ->
        val token = CancellationTokenSource()
        cont.invokeOnCancellation { token.cancel() }

        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, token.token)
            .addOnSuccessListener { loc -> cont.resume(loc) }
            .addOnFailureListener { cont.resume(null) }
    }

    private suspend fun awaitLastLocation(
        client: com.google.android.gms.location.FusedLocationProviderClient
    ): Location? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc) }
            .addOnFailureListener { cont.resume(null) }
    }

    private fun Location.toCoarse(): CoarseLocation =
        CoarseLocation(latitude, longitude, accuracy)
}