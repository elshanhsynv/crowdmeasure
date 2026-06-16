package com.crowdmeasure.sdk.upload

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crowdmeasure.sdk.CrowdMeasureSdk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadClientInstrumentedTest {
    @Test
    fun validatesPersistsAndDisablesSettings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val client = CrowdMeasureUploads.install(
            context,
            CrowdMeasureSdk.create(context),
            MeasurementUploader { MeasurementUploaderResult.Success(UploadBatchResult()) },
        )

        assertTrue(client.enable(19, true) is MeasurementUploadResult.Failure)
        assertTrue(client.enable(60, false) is MeasurementUploadResult.Success)
        assertEquals(MeasurementUploadSettings(true, 60, false), client.observeStatus().first().settings)
        assertTrue(client.disable() is MeasurementUploadResult.Success)
        assertEquals(false, client.observeStatus().first().settings.enabled)
    }
}
