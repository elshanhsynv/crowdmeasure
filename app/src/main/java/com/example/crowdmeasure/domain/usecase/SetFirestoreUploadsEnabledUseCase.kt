package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.yourcompany.crowdmeasure.sdk.upload.MeasurementUploadClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SetFirestoreUploadsEnabledUseCase @Inject constructor(
    private val session: UserSessionRepository,
    private val uploads: MeasurementUploadClient,
) {
    suspend operator fun invoke(enabled: Boolean) {
        session.setFirestoreUploadsEnabled(enabled)
        if (enabled) uploads.enable(60, true) else uploads.disable()
    }
}
