package com.crowdmeasure.sdk.internal.measurement.collectors

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.crowdmeasure.sdk.model.Location as CoarseLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

object LocationCollector {
    private const val OVERALL_TIMEOUT_MS = 10_000L
    private const val STRATEGY_TIMEOUT_MS = 6_000L
    private const val CURRENT_LOCATION_TIMEOUT_MS = 5_000L

    /**
     * Best-effort coarse location. Returns null if:
     *  - permission not granted
     *  - location services disabled
     *  - no fix obtained within [OVERALL_TIMEOUT_MS]
     *
     * Permission must be checked by the caller before invoking; this function
     * re-checks defensively and returns null rather than throwing.
     */
    @SuppressLint("MissingPermission")
    suspend fun tryGetCoarseOneShot(context: Context): CoarseLocation? {
        if (!hasCoarsePermission(context)) return null
        if (!isLocationServicesEnabled(context)) return null

        return withTimeoutOrNull(OVERALL_TIMEOUT_MS.milliseconds) {
            // Strategy 1: Fused last-known / current (lowest latency, no active scan)
            tryFused(context)
            // Strategy 2: Platform NETWORK_PROVIDER last-known, then single update
                ?: tryPlatformNetworkProvider(context)
                // Strategy 3: Fused single-update (triggers active scan; highest latency)
                ?: tryFusedSingleUpdate(context)
        }
    }

    private fun hasCoarsePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationServicesEnabled(context: Context): Boolean =
        context.getSystemService<LocationManager>()
            ?.runCatching { isLocationEnabled }
            ?.getOrDefault(false)
            ?: false

    // -------------------------------------------------------------------------
    // Strategy 1: Fused Provider (last-known then current)
    // -------------------------------------------------------------------------

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private suspend fun tryFused(context: Context): CoarseLocation? {
        val client = LocationServices.getFusedLocationProviderClient(context)
         return awaitLastLocation(context, client)?.toCoarse()
            ?: awaitCurrentLocation(context, client)?.toCoarse()
    }

    private suspend fun awaitLastLocation(
        context: Context,
        client: FusedLocationProviderClient,
    ): Location? = suspendCancellableCoroutine { cont ->

        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (cont.isActive) cont.resume(location)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        } catch (e: SecurityException) {
            if (cont.isActive) cont.resume(null)
        }
    }

    private suspend fun awaitCurrentLocation(
        context: Context,
        client: FusedLocationProviderClient,
    ): Location? = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS.milliseconds) {

        val hasPermission =
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withTimeoutOrNull null

        suspendCancellableCoroutine { cont ->
            val tokenSource = CancellationTokenSource()

            cont.invokeOnCancellation {
                tokenSource.cancel()
            }

            try {
                client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    tokenSource.token
                )
                    .addOnSuccessListener { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }

            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }
    // -------------------------------------------------------------------------
    // Strategy 2: Platform LocationManager / NETWORK_PROVIDER
    // -------------------------------------------------------------------------

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private suspend fun tryPlatformNetworkProvider(context: Context): CoarseLocation? {
        val lm = context.getSystemService<LocationManager>() ?: return null

        // Last-known is instant and very common to be present
        val lastKnown = runCatching {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
        if (lastKnown != null) return lastKnown.toCoarse()

        // No cached fix — request one active update with a timeout
        return withTimeoutOrNull(STRATEGY_TIMEOUT_MS.milliseconds) {
            suspendCancellableCoroutine { cont ->
                val done = AtomicBoolean(false)

                fun finish(loc: Location?) {
                    if (done.compareAndSet(false, true)) {
                        cont.resume(loc?.toCoarse())
                    }
                }

                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { lm.removeUpdates(this) }
                        finish(location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(
                        provider: String?,
                        status: Int,
                        extras: android.os.Bundle?,
                    ) = Unit
                }

                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }

                runCatching {
                    @Suppress("DEPRECATION")
                    lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        /* minTimeMs = */ 0L,
                        /* minDistanceM = */ 0f,
                        listener,
                        Looper.getMainLooper(),
                    )
                }.onFailure { finish(null) }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Strategy 3: Fused single-update (active scan — highest latency/battery)
    // -------------------------------------------------------------------------

    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private suspend fun tryFusedSingleUpdate(context: Context): CoarseLocation? {
        val client = LocationServices.getFusedLocationProviderClient(context)

        return withTimeoutOrNull(STRATEGY_TIMEOUT_MS.milliseconds) {
            suspendCancellableCoroutine { cont ->
                val done = AtomicBoolean(false)

                fun finish(loc: Location?) {
                    if (done.compareAndSet(false, true)) {
                        cont.resume(loc?.toCoarse())
                    }
                }

                val request = LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    /* intervalMillis = */ 2_000L,
                ).setMaxUpdates(1).build()

                val callback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(
                        result: com.google.android.gms.location.LocationResult,
                    ) {
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

    private fun Location.toCoarse(): CoarseLocation =
        CoarseLocation(latitude, longitude, accuracy)
}