package com.crowdmeasure.sdk.firestore.calls

import com.crowdmeasure.sdk.calls.CallCellSample
import com.crowdmeasure.sdk.calls.CallSession
import com.crowdmeasure.sdk.calls.CallSource
import com.crowdmeasure.sdk.calls.CallType
import com.crowdmeasure.sdk.calls.CallUploadItem
import com.crowdmeasure.sdk.model.CarrierInfo
import com.crowdmeasure.sdk.model.CellInfo
import com.crowdmeasure.sdk.model.DataUsageInfo
import com.crowdmeasure.sdk.model.Location
import com.crowdmeasure.sdk.model.NrState
import org.junit.Assert.assertEquals
import org.junit.Test

class CallFirestorePayloadTest {
    @Test
    fun sampleIncludesLocationAndDataUsage() {
        val payload = CallFirestorePayload.sample(
            CallCellSample(
                id = 1,
                sessionId = "session",
                sampledAtUtcMs = 1_000,
                elapsedMs = 100,
                cell = CellInfo(rat = "LTE", nrState = NrState.NONE, serving = null),
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
                dataUsage = DataUsageInfo(dlMB = 1.0, ulMB = 0.5, dlKbps = 800.0, ulKbps = 400.0),
            )
        )

        @Suppress("UNCHECKED_CAST")
        val location = payload["location"] as Map<String, Any?>
        assertEquals(40.4093, location["lat"])
        assertEquals(49.8671, location["lon"])
        assertEquals(12f, location["accuracyMeters"])

        @Suppress("UNCHECKED_CAST")
        val dataUsage = payload["data_usage"] as Map<String, Any?>
        assertEquals(1.0, dataUsage["dl_mb"])
        assertEquals(0.5, dataUsage["ul_mb"])
        assertEquals(800.0, dataUsage["dl_kbps"])
        assertEquals(400.0, dataUsage["ul_kbps"])
    }

    @Test
    fun sessionIncludesCarriers() {
        val payload = CallFirestorePayload.session(
            CallUploadItem(
                session = CallSession(
                    sessionId = "session",
                    startedAtUtcMs = 1_000,
                    endedAtUtcMs = 2_000,
                    callType = CallType.UNKNOWN,
                    callSource = CallSource.VOIP_GENERIC,
                    sampleIntervalSeconds = 5,
                    sampleCount = 1,
                    endReason = "call_ended",
                    simCarriers = listOf(
                        CarrierInfo(
                            carrierName = "Test",
                            mcc = "001",
                            mnc = "01",
                            simOperatorId = "00101",
                            simOperatorName = "Test",
                            countryIso = "az",
                            duplexMode = "FDD",
                        )
                    ),
                ),
                samples = emptyList(),
                installId = "install",
                deviceModel = "device",
            ),
            uploadedAt = "now",
        )

        @Suppress("UNCHECKED_CAST")
        val carriers = payload["sim_carriers"] as List<Map<String, Any?>>
        assertEquals("Test", carriers.single()["carrierName"])
    }
}
