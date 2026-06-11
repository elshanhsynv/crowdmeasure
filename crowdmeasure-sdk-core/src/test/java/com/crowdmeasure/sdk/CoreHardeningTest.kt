package com.crowdmeasure.sdk

import com.crowdmeasure.sdk.internal.measurement.collectors.IpCollector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreHardeningTest {
    @Test
    fun ipHashIsSaltedAndNeverContainsRawIp() {
        val first = IpCollector.hashIp("203.0.113.10", "install-a")
        val second = IpCollector.hashIp("203.0.113.10", "install-b")

        assertEquals(64, first.length)
        assertNotEquals(first, second)
        assertTrue("203.0.113.10" !in first)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidProbeAttemptsFailFast() {
        PerformanceProbeConfig(attempts = 0)
    }
}
