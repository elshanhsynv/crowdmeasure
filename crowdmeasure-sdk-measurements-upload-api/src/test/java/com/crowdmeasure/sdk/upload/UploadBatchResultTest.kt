package com.crowdmeasure.sdk.upload

import org.junit.Test

class UploadBatchResultTest {
    @Test(expected = IllegalArgumentException::class)
    fun partialOutcomeIdsCannotOverlap() {
        UploadBatchResult(uploadedIds = setOf("same"), retryableIds = setOf("same"))
    }
}
