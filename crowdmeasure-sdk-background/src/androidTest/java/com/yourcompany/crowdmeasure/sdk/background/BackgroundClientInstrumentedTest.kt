package com.yourcompany.crowdmeasure.sdk.background

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourcompany.crowdmeasure.sdk.CrowdMeasureSdk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundClientInstrumentedTest {
    @Test
    fun validatesPersistsAndDisablesSettings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val client = CrowdMeasureBackground.install(context, CrowdMeasureSdk.create(context))

        assertTrue(client.enable(19, false) is BackgroundResult.Failure)
        assertTrue(client.enable(60, true) is BackgroundResult.Success)
        assertEquals(BackgroundCollectionSettings(true, 60, true), client.observeSettings().first())

        assertTrue(client.disable() is BackgroundResult.Success)
        assertEquals(false, client.observeSettings().first().enabled)
    }
}
