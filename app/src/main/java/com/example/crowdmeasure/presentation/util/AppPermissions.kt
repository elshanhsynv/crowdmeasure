package com.example.crowdmeasure.presentation.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.location.LocationManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object AppPermissions {

    fun hasCoarseLocation(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun hasPhoneState(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED

    fun hasFineLocation(context: Context): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = try {
            context.getSystemService(LocationManager::class.java)
        } catch (e: Throwable) {
            null
        } ?: return false

        return try {
            locationManager.isLocationEnabled
        } catch (_: Throwable) {
            val gps = runCatching {
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            }.getOrDefault(false)
            val network = runCatching {
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            }.getOrDefault(false)
            gps || network
        }
    }

    fun locationServicesEnabledFlow(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext

        fun emit() {
            trySend(AppPermissions.isLocationServicesEnabled(appContext))
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                emit()
            }
        }

        // Fires when user toggles location providers / location master switch (varies by device)
        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }

        appContext.registerReceiver(receiver, filter)
        emit() // initial value

        awaitClose { appContext.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}
