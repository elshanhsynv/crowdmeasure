package com.crowdmeasure.sdk.internal.measurement.collectors

import android.telephony.CellInfo
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import com.crowdmeasure.sdk.model.NrState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelephonyCollectorLogicTest {
    @Test
    fun lteWithUnregisteredNrEvidenceDoesNotReportNsa() {
        val state = TelephonyCollectorLogic.deriveNrState(
            dataNetworkType = TelephonyManager.NETWORK_TYPE_LTE,
            displayNetworkType = null,
            displayOverrideNetworkType = null,
            hasRegisteredNr = false,
        )

        assertEquals(NrState.NONE, state)
    }

    @Test
    fun lteWithRegisteredNrReportsNsa() {
        val state = TelephonyCollectorLogic.deriveNrState(
            dataNetworkType = TelephonyManager.NETWORK_TYPE_LTE,
            displayNetworkType = null,
            displayOverrideNetworkType = null,
            hasRegisteredNr = true,
        )

        assertEquals(NrState.NSA, state)
    }

    @Test
    fun displayInfoNsaOverrideReportsNsa() {
        val state = TelephonyCollectorLogic.deriveNrState(
            dataNetworkType = TelephonyManager.NETWORK_TYPE_LTE,
            displayNetworkType = TelephonyManager.NETWORK_TYPE_LTE,
            displayOverrideNetworkType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
            hasRegisteredNr = false,
        )

        assertEquals(NrState.NSA, state)
    }

    @Test
    fun noLocationCoarseRatCanUseDataOrDisplayNetworkType() {
        assertEquals(
            "NR",
            TelephonyCollectorLogic.coarseRatName(
                dataNetworkType = TelephonyManager.NETWORK_TYPE_NR,
                displayNetworkType = null,
            ),
        )

        assertEquals(
            "NR",
            TelephonyCollectorLogic.coarseRatName(
                dataNetworkType = null,
                displayNetworkType = TelephonyManager.NETWORK_TYPE_NR,
            ),
        )

        assertNull(
            TelephonyCollectorLogic.coarseRatName(
                dataNetworkType = null,
                displayNetworkType = null,
            )
        )
    }

    @Test
    fun servingSelectionPrefersFreshRegisteredCompatibleCell() {
        val staleStrongLte = CellSelectionCandidate(
            rat = TelephonyRat.LTE,
            registered = true,
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            ageMs = TelephonyCollectorLogic.MAX_CELL_AGE_MS + 1,
            signalDbm = -70,
        )
        val freshWeakLte = CellSelectionCandidate(
            rat = TelephonyRat.LTE,
            registered = true,
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            ageMs = 1_000L,
            signalDbm = -110,
        )

        assertEquals(
            1,
            TelephonyCollectorLogic.selectServingIndex(
                nrState = NrState.NSA,
                candidates = listOf(staleStrongLte, freshWeakLte),
            ),
        )
    }

    @Test
    fun servingSelectionDoesNotFallbackToUnrelatedRatForSa() {
        val registeredLte = CellSelectionCandidate(
            rat = TelephonyRat.LTE,
            registered = true,
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            ageMs = 1_000L,
            signalDbm = -70,
        )

        assertNull(
            TelephonyCollectorLogic.selectServingIndex(
                nrState = NrState.SA,
                candidates = listOf(registeredLte),
            )
        )
    }

    @Test
    fun servingSelectionPrefersPrimaryServingOverStrongerSecondary() {
        val primaryServing = CellSelectionCandidate(
            rat = TelephonyRat.LTE,
            registered = true,
            connectionStatus = CellInfo.CONNECTION_PRIMARY_SERVING,
            ageMs = 2_000L,
            signalDbm = -105,
        )
        val secondaryServing = CellSelectionCandidate(
            rat = TelephonyRat.LTE,
            registered = true,
            connectionStatus = CellInfo.CONNECTION_SECONDARY_SERVING,
            ageMs = 1_000L,
            signalDbm = -60,
        )

        assertEquals(
            0,
            TelephonyCollectorLogic.selectServingIndex(
                nrState = NrState.NSA,
                candidates = listOf(primaryServing, secondaryServing),
            ),
        )
    }
}
