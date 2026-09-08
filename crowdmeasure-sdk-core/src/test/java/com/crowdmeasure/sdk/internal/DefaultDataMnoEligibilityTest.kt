package com.crowdmeasure.sdk.internal

import com.crowdmeasure.sdk.DefaultDataMnoEligibilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultDataMnoEligibilityTest {
    @Test
    fun unrestrictedWhenNoTargetIsConfigured() {
        val result = classifyDefaultDataMnoEligibility(null, "40001")

        assertEquals(DefaultDataMnoEligibilityState.UNRESTRICTED, result.state)
        assertTrue(result.allowsCollection)
    }

    @Test
    fun trimsAndMatchesConfiguredTarget() {
        val result = classifyDefaultDataMnoEligibility(" 40001 ", "40001")

        assertEquals(DefaultDataMnoEligibilityState.MATCHED, result.state)
        assertTrue(result.allowsCollection)
        assertEquals("40001", result.requiredMnoId)
    }

    @Test
    fun blocksMismatchedOrUnavailableDefaultMno() {
        val mismatched = classifyDefaultDataMnoEligibility("40001", "40002")
        val unavailable = classifyDefaultDataMnoEligibility("40001", null)

        assertEquals(DefaultDataMnoEligibilityState.MISMATCHED, mismatched.state)
        assertFalse(mismatched.allowsCollection)
        assertEquals(DefaultDataMnoEligibilityState.UNAVAILABLE, unavailable.state)
        assertFalse(unavailable.allowsCollection)
    }

    @Test
    fun blocksRoamingWhenRequired() {
        val roaming = classifyDefaultDataMnoEligibility(
            requiredMnoId = "40001",
            defaultDataMnoId = "40001",
            requireNotRoaming = true,
            defaultDataRoaming = true,
        )
        val notRoaming = classifyDefaultDataMnoEligibility(
            requiredMnoId = null,
            defaultDataMnoId = null,
            requireNotRoaming = true,
            defaultDataRoaming = false,
        )
        val unavailable = classifyDefaultDataMnoEligibility(
            requiredMnoId = null,
            defaultDataMnoId = null,
            requireNotRoaming = true,
            defaultDataRoaming = null,
        )

        assertEquals(DefaultDataMnoEligibilityState.ROAMING, roaming.state)
        assertFalse(roaming.allowsCollection)
        assertEquals(DefaultDataMnoEligibilityState.NOT_ROAMING, notRoaming.state)
        assertTrue(notRoaming.allowsCollection)
        assertEquals(DefaultDataMnoEligibilityState.UNAVAILABLE, unavailable.state)
        assertFalse(unavailable.allowsCollection)
    }
}
