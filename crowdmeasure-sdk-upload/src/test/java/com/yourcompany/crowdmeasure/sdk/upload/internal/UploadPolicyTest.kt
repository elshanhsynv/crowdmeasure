package com.crowdmeasure.sdk.upload.internal

import com.crowdmeasure.sdk.upload.MeasurementUploadError
import com.crowdmeasure.sdk.upload.UploadRunCode
import org.junit.Assert.assertEquals
import org.junit.Test

class UploadPolicyTest {
    @Test fun mapsTypedErrorsToStatusCodes() {
        assertEquals(UploadRunCode.TRANSIENT_FAILURE, MeasurementUploadError.TransientFailure().toCode())
        assertEquals(UploadRunCode.BACKEND_REJECTED, MeasurementUploadError.BackendRejected().toCode())
        assertEquals(UploadRunCode.SERIALIZATION_FAILED, MeasurementUploadError.SerializationFailure().toCode())
    }
}
