package com.crowdmeasure.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreHardeningTest {
    @Test
    fun publicIpCollectionDefaultsToRawIp() {
        assertEquals(PublicIpPolicy.RAW, CrowdMeasureConfig().publicIpPolicy)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidProbeAttemptsFailFast() {
        PerformanceProbeConfig(attempts = 0)
    }

    @Test
    fun measurementRequirementsCanRunRequiresSupportedAndroidAndPermissions() {
        assertTrue(
            MeasurementRequirements(
                supportedAndroidVersion = true,
                locationServicesEnabled = true,
                missingPermissions = emptySet(),
            ).canRun
        )

        assertTrue(
            MeasurementRequirements(
                supportedAndroidVersion = true,
                locationServicesEnabled = false,
                missingPermissions = emptySet(),
            ).canRun
        )

        assertTrue(
            !MeasurementRequirements(
                supportedAndroidVersion = true,
                locationServicesEnabled = true,
                missingPermissions = setOf("android.permission.ACCESS_FINE_LOCATION"),
            ).canRun
        )
    }
}
