package com.crowdmeasure.sdk.firestore.calls

import com.crowdmeasure.sdk.calls.CallCellSample
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.Location
import com.crowdmeasure.sdk.model.NrState
import org.junit.Assert.assertEquals
import org.junit.Test

class CallFirestorePayloadTest {
    @Test
    fun sampleIncludesLocation() {
        val payload = CallFirestorePayload.sample(
            CallCellSample(
                id = 1,
                sessionId = "session",
                sampledAtUtcMs = 1_000,
                elapsedMs = 100,
                cell = CellInfo(rat = "LTE", nrState = NrState.NONE, serving = null, aggregation = null),
                rat = "LTE",
                nrState = "NONE",
                dbm = null,
                rsrpDbm = null,
                rsrqDb = null,
                sinrDb = null,
                pci = null,
                tac = null,
                band = null,
                location = Location(40.4093, 49.8671, 12f),
            )
        )

        @Suppress("UNCHECKED_CAST")
        val location = payload["location"] as Map<String, Any?>
        assertEquals(40.4093, location["lat"])
        assertEquals(49.8671, location["lon"])
        assertEquals(12f, location["accuracyMeters"])
    }
}
