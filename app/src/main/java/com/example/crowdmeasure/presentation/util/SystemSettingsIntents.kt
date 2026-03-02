package com.example.crowdmeasure.presentation.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object SystemSettingsIntents {

    /**
     * Opens a relevant battery optimization screen.
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val appContext = context.applicationContext

        val candidates = listOf(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", appContext.packageName, null)
            }
        )

        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(appContext.packageManager) != null) {
                appContext.startActivity(intent)
                return
            }
        }
    }
}
