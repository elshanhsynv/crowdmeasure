package com.crowdmeasure.sdk

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crowdmeasure.sdk.internal.DefaultMeasurementStore
import com.crowdmeasure.sdk.model.DeviceEnvironment
import com.crowdmeasure.sdk.model.EnvironmentInfo
import com.crowdmeasure.sdk.model.IpInfo
import com.crowdmeasure.sdk.model.Measurement
import com.crowdmeasure.sdk.model.Meta
import com.crowdmeasure.sdk.model.NetworkEnvironment
import com.crowdmeasure.sdk.model.PerformanceInfo
import com.crowdmeasure.sdk.model.TransportType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultStorageInstrumentedTest {
    @Test
    fun defaultStorePersistsExportsAndDeletesMeasurements() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val databaseName = "sdk-instrumented-test.db"
        context.deleteDatabase(databaseName)
        val store = DefaultMeasurementStore.create(context, databaseName)
        val sdk = CrowdMeasureSdk.create(context, measurementStore = store)

        store.save(testMeasurement())
        assertEquals("test-id", sdk.measurements.observeLatest().first()?.meta?.measurementId)
        assertEquals(1, sdk.measurements.observeHistory().first().size)
        assertEquals(1, sdk.queue.observeStatus().first().pendingCount)
        assertEquals("test-id", sdk.queue.getCandidates().single().meta.measurementId)
        sdk.queue.markUploaded(listOf("test-id"))
        assertEquals(0, sdk.queue.observeStatus().first().pendingCount)
        assertEquals(true, sdk.data.exportMeasurements(10) is CrowdMeasureResult.Success)
        assertEquals(
            1,
            (sdk.data.pruneExpiredMeasurements(Long.MAX_VALUE) as CrowdMeasureResult.Success).value,
        )

        sdk.data.deleteAllMeasurements()
        assertNull(sdk.measurements.observeLatest().first())
        context.deleteDatabase(databaseName)
    }

    private fun testMeasurement() = Measurement(
        meta = Meta(
            measurementId = "test-id",
            timestampUtcMs = 1L,
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
