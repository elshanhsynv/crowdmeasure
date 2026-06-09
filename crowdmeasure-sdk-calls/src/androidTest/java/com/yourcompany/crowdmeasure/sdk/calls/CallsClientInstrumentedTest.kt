package com.yourcompany.crowdmeasure.sdk.calls

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallsClientInstrumentedTest {
    @Test
    fun installDoesNotEnableFeatures() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = CrowdMeasureCalls.install(
            context,
            CrowdMeasureSdk.create(context),
            CallSamplingConfig(notificationIconResId = android.R.drawable.ic_dialog_info),
        )
        client.setCellularSamplingEnabled(false)
        client.setVoipSamplingEnabled(false)
        client.setUploadsEnabled(false)
        val settings = client.observeSettings().first()
        assertFalse(settings.cellularEnabled)
        assertFalse(settings.voipEnabled)
        assertFalse(settings.uploadsEnabled)
        assertTrue(client.activateEnabledFeatures() is CallSamplingResult.Success)
    }
}
