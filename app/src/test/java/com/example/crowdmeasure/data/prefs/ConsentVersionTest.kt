package com.example.crowdmeasure.data.prefs

import androidx.datastore.preferences.core.preferencesOf
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsentVersionTest {

    @Test
    fun freshInstall_hasNotAccepted() {
        assertEquals(0, preferencesOf().consentAcceptedVersion())
    }

    @Test
    fun legacyDismissedGate_countsAsVersion1() {
        val prefs = preferencesOf(DataStoreKeys.CONSENT_GATE_DISMISSED to true)
        assertEquals(1, prefs.consentAcceptedVersion())
    }

    @Test
    fun storedVersion_winsOverLegacyFlag() {
        val prefs = preferencesOf(
            DataStoreKeys.CONSENT_GATE_DISMISSED to true,
            DataStoreKeys.CONSENT_ACCEPTED_VERSION to 3,
        )
        assertEquals(3, prefs.consentAcceptedVersion())
    }
}
