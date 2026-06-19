package com.crowdmeasure.sdk.calls.upload.internal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crowdmeasure.sdk.calls.CallUploadBatchResult
import com.crowdmeasure.sdk.calls.CallUploader
import com.crowdmeasure.sdk.calls.CallUploaderResult
import com.crowdmeasure.sdk.calls.upload.CallUploadConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallUploadDataStoreInstrumentedTest {
    private val uploader = CallUploader {
        CallUploaderResult.Success(CallUploadBatchResult())
    }

    @Test
    fun samePreferencesFileCanBeReusedByMultipleClients() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = CallUploadConfig(
            preferencesName = "test_calls_upload_datastore_reuse",
            uploader = uploader,
        )

        val first = CallUploadClientImpl(context, config)
        val second = CallUploadClientImpl(context, config)

        assertFalse(first.observeSettings().first().enabled)
        assertFalse(second.observeSettings().first().enabled)
    }
}