package com.crowdmeasure.sdk.calls

import com.crowdmeasure.sdk.DefaultDataMnoEligibility
import com.crowdmeasure.sdk.DefaultDataMnoEligibilityState
import org.junit.Assert.assertFalse
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
//            batteryOptimizationIgnored = false,
        )
        assertTrue(requirements.canStart)
    }

    @Test
    fun targetMnoMismatchBlocksCallSampling() {
        val requirements = CallSamplingRequirements(
            supportedAndroidVersion = true,
            phoneStateGranted = true,
            fineLocationGranted = true,
            backgroundLocationGranted = true,
            locationServicesEnabled = true,
            notificationGranted = true,
            defaultDataMnoEligibility = DefaultDataMnoEligibility(
                state = DefaultDataMnoEligibilityState.MISMATCHED,
                requiredMnoId = "40001",
                defaultDataMnoId = "40002",
            ),
        )

        assertFalse(requirements.canStart)
    }
}
