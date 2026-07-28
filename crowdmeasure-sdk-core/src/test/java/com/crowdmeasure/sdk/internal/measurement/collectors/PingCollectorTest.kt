package com.crowdmeasure.sdk.internal.measurement.collectors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PingCollectorTest {
    @Test
    fun resultFromSamplesCalculatesPingStats() {
        val result = PingResult.from(
            samples = listOf(10L, 25L, 15L),
            failures = 2,
            attempts = 5,
        )

        assertEquals(17L, result.avgMs)
        assertEquals(10L, result.minMs)
        assertEquals(25L, result.maxMs)
        assertEquals(13L, result.jitterMs)
        assertEquals(40.0, result.packetLossPct, 0.0)
    }

    @Test
    fun failedResultReportsTotalPacketLossWithoutSamples() {
        val result = PingResult.failed(attempts = 5)

        assertNull(result.avgMs)
        assertNull(result.minMs)
        assertNull(result.maxMs)
        assertNull(result.jitterMs)
        assertEquals(100.0, result.packetLossPct, 0.0)
    }
}
