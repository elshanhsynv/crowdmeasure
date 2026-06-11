package com.crowdmeasure.sdk.background.internal

import com.crowdmeasure.sdk.background.BackgroundWorkState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundPolicyTest {
    @Test
    fun validatesSupportedIntervals() {
        assertFalse(isValidBackgroundInterval(19))
        assertTrue(isValidBackgroundInterval(20))
        assertTrue(isValidBackgroundInterval(10_080))
        assertFalse(isValidBackgroundInterval(10_081))
    }

    @Test
    fun preventsRunsInsideConfiguredInterval() {
        assertTrue(shouldSkipRecentRun(1_000, 1_000 + 59 * 60_000, 60))
        assertFalse(shouldSkipRecentRun(1_000, 1_000 + 60 * 60_000, 60))
        assertFalse(shouldSkipRecentRun(null, 1_000, 60))
    }

    @Test
    fun mapsWorkStatesAndUnknownValues() {
        assertEquals(BackgroundWorkState.RUNNING, mapWorkState("RUNNING"))
        assertEquals(BackgroundWorkState.UNKNOWN, mapWorkState(null))
        assertEquals(BackgroundWorkState.UNKNOWN, mapWorkState("NEW_STATE"))
    }
}
