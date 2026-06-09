package com.yourcompany.crowdmeasure.sdk.firestore

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourcompany.crowdmeasure.sdk.model.DeviceEnvironment
import com.yourcompany.crowdmeasure.sdk.model.EnvironmentInfo
import com.yourcompany.crowdmeasure.sdk.model.IpInfo
import com.yourcompany.crowdmeasure.sdk.model.Measurement
import com.yourcompany.crowdmeasure.sdk.model.Meta
import com.yourcompany.crowdmeasure.sdk.model.NetworkEnvironment
import com.yourcompany.crowdmeasure.sdk.model.PerformanceInfo
import com.yourcompany.crowdmeasure.sdk.model.TransportType
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadItem
import com.yourcompany.crowdmeasure.sdk.calls.*
import com.yourcompany.crowdmeasure.sdk.model.CellInfo
import com.yourcompany.crowdmeasure.sdk.model.NrState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestorePayloadInstrumentedTest {
    @Test
    fun preservesExistingMeasurementDocumentContract() {
        val payload = FirestorePayloadFactory.create(
            MeasurementUploadItem(measurement(), "install-id"),
            nowUtcMs = 2_000L,
        )

        assertEquals(
            setOf("install_id", "measurement_id", "transport", "run_date", "uploaded_at", "payload"),
            payload.keys,
        )
        assertEquals("install-id", payload["install_id"])
        assertEquals("measurement-id", payload["measurement_id"])
        assertEquals("OTHER", payload["transport"])
        assertTrue(payload["payload"] is Map<*, *>)
    }

    @Test
    fun preservesExistingCallDocumentContract() {
        val item = CallUploadItem(
            CallSession("session-id", 1L, 2L, CallType.INCOMING, CallSource.CELLULAR, 5, 1, "call_ended"),
            listOf(CallCellSample(7L, "session-id", 2L, 1L, CellInfo(rat = null, nrState = NrState.NONE, serving = null, aggregation = null), null, "NONE", null, null, null, null, null, null, null)),
            "install-id",
            "device",
        )
        val session = CallFirestorePayloadFactory.session(item, uploadedAt = "server-time")
        val sample = CallFirestorePayloadFactory.sample(item.samples.single())
        assertEquals(setOf("schema_version", "session_id", "install_id", "device_model", "started_at_utc_ms", "ended_at_utc_ms", "call_type", "call_source", "sample_interval_seconds", "sample_count", "end_reason", "uploaded_at"), session.keys)
        assertEquals(setOf("sample_id", "sampled_at_utc_ms", "elapsed_ms", "rat", "nr_state", "dbm", "rsrp_dbm", "rsrq_db", "sinr_db", "pci", "tac", "band", "cell"), sample.keys)
    }

    private fun measurement() = Measurement(
        meta = Meta(
            measurementId = "measurement-id",
            timestampUtcMs = 1_000L,
            appVersion = "1",
            androidRelease = "10",
            androidSdk = 29,
            deviceModel = "test",
            brand = "test",
            deviceManufacturer = "test",
            deviceOS = "Android",
            buildID = "test",
            hardware = "test",
            chipset = "test",
            chipsetManufacturer = "test",
            sessionId = null,
            userIdHash = null,
        ),
        environment = EnvironmentInfo(
            location = null,
            network = NetworkEnvironment(
                transport = TransportType.OTHER,
                ip = IpInfo(),
                validatedInternet = null,
                captivePortal = null,
                vpn = null,
                metered = null,
                wifi = null,
                cell = null,
                dataUsage = null,
            ),
            device = DeviceEnvironment(
                batteryPct = null,
                charging = false,
                batterySaver = null,
                screenOn = false,
                dozeMode = null,
                dataSaver = null,
                thermalState = null,
                cpuUsagePct = null,
                memoryUsagePct = null,
            ),
        ),
        performance = PerformanceInfo(endpointId = "https://example.com/"),
    )
}
