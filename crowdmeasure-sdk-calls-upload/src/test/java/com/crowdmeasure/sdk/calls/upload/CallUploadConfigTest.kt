package com.crowdmeasure.sdk.calls.upload

import com.crowdmeasure.sdk.calls.CallUploader
import com.crowdmeasure.sdk.calls.CallUploaderResult
import com.crowdmeasure.sdk.calls.CallUploadBatchResult
import org.junit.Test

class CallUploadConfigTest {
    private val uploader = CallUploader {
        CallUploaderResult.Success(CallUploadBatchResult())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBatchSizeFailsFast() {
        CallUploadConfig(
            defaultBatchSize = 0,
            uploader = uploader,
        )
    }
}
