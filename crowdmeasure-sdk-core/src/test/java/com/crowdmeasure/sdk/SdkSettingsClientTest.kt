package com.crowdmeasure.sdk

import com.crowdmeasure.sdk.internal.SdkSettingsClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SdkSettingsClientTest {
    private val store = FakeSettingsStore()
    private val client = SdkSettingsClient(store)

    @Test
    fun rejectsNonHttpsEndpoint() = runBlocking {
        val result = client.setEndpointUrl("http://example.com")

        assertTrue(result is CrowdMeasureResult.Failure)
        assertTrue((result as CrowdMeasureResult.Failure).error is CrowdMeasureError.InvalidConfiguration)
    }

    @Test
    fun persistsValidEndpoint() = runBlocking {
        val result = client.setEndpointUrl("https://example.com/probe")

        assertTrue(result is CrowdMeasureResult.Success)
        assertEquals("https://example.com/probe", store.current.value.endpointUrl)
    }

    @Test
    fun rejectsRetentionOutsideSupportedRange() = runBlocking {
        val result = client.setRetentionDays(0)

        assertTrue(result is CrowdMeasureResult.Failure)
        assertTrue((result as CrowdMeasureResult.Failure).error is CrowdMeasureError.InvalidConfiguration)
    }
}

private class FakeSettingsStore : CrowdMeasureSettingsStore {
    val current = MutableStateFlow(CrowdMeasureSettings("https://www.google.com/", 7))
    override val settings: Flow<CrowdMeasureSettings> = current

    override suspend fun setEndpointUrl(url: String) {
        current.value = current.value.copy(endpointUrl = url)
    }

    override suspend fun setRetentionDays(days: Int) {
        current.value = current.value.copy(retentionDays = days)
    }
}
