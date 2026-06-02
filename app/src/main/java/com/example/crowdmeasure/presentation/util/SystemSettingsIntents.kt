package com.example.crowdmeasure.presentation.util

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast

object SystemSettingsIntents {

    fun openLocationSettings(context: Context) {
        val appContext = context.applicationContext
        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .takeIf { it.resolveActivity(appContext.packageManager) != null }
            ?.let(appContext::startActivity)
    }

    /**
     * Requests the user to allow this app to ignore battery optimizations.
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val appContext = context.applicationContext

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${appContext.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            if (intent.resolveActivity(appContext.packageManager) != null) {
                appContext.startActivity(intent)
            } else {
                Toast.makeText(
                    appContext,
                    "Could not open battery settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                appContext,
                "Could not open battery settings",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}