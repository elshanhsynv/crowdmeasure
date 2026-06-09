package com.yourcompany.crowdmeasure.sdk.calls

import org.junit.Assert.assertTrue
import org.junit.Test

class CallRequirementsTest {
    @Test
    fun batteryOptimizationIsWarningOnly() {
        val requirements = CallSamplingRequirements(
            supportedAndroidVersion = true,
            phoneStateGranted = true,
            fineLocationGranted = true,
            backgroundLocationGranted = true,
            locationServicesEnabled = true,
            notificationGranted = true,
            batteryOptimizationIgnored = false,
        )
        assertTrue(requirements.canStart)
    }
}
