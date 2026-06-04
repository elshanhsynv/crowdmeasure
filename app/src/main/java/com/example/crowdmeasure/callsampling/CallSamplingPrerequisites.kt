package com.example.crowdmeasure.callsampling

import android.content.Context
import com.example.crowdmeasure.presentation.util.AppPermissions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class CallSamplingPrerequisiteState(
    val phoneStateGranted: Boolean,
    val fineLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
//    val locationServicesEnabled: Boolean,
    val notificationGranted: Boolean,
//    val batteryOptimizationIgnored: Boolean
) {
    val canStart: Boolean =
        phoneStateGranted &&
            fineLocationGranted &&
            backgroundLocationGranted &&
//            locationServicesEnabled &&
//            batteryOptimizationIgnored
            notificationGranted

    val missingReason: String
        get() = when {
            !phoneStateGranted -> "missing_read_phone_state"
            !fineLocationGranted -> "missing_fine_location"
            !backgroundLocationGranted -> "missing_background_location"
//            !locationServicesEnabled -> "location_services_off"
            !notificationGranted -> "missing_post_notifications"
//            !batteryOptimizationIgnored -> "battery_optimization_not_ignored"
            else -> "ready"
        }
}

@Singleton
class CallSamplingPrerequisites @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun evaluate(): CallSamplingPrerequisiteState =
        CallSamplingPrerequisiteState(
            phoneStateGranted = AppPermissions.hasPhoneState(context),
            fineLocationGranted = AppPermissions.hasFineLocation(context),
            backgroundLocationGranted = AppPermissions.hasBackgroundLocation(context),
//            locationServicesEnabled = AppPermissions.isLocationServicesEnabled(context),
            notificationGranted = AppPermissions.hasPostNotifications(context),
//            batteryOptimizationIgnored = AppPermissions.ignoresBatteryOptimizations(context)
        )
}
