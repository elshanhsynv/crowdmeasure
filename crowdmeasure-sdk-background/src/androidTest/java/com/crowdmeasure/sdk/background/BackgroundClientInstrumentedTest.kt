package com.crowdmeasure.sdk.background

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crowdmeasure.sdk.CrowdMeasureSdk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundClientInstrumentedTest {
    @Test
    fun validatesPersistsAndDisablesSettings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = CrowdMeasureBackground.install(context, CrowdMeasureSdk.create(context))

        Assert.assertTrue(client.enable(19, false) is BackgroundResult.Failure)
        Assert.assertTrue(client.enable(60, true) is BackgroundResult.Success)
        Assert.assertEquals(
            BackgroundCollectionSettings(true, 60, true),
            client.observeSettings().first()
        )

        Assert.assertTrue(client.disable() is BackgroundResult.Success)
        Assert.assertEquals(false, client.observeSettings().first().enabled)
    }
}