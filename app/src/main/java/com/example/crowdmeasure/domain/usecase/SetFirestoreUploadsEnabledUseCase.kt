package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

/** Turns uploads of both measurements and call sessions on or off. */
class SetFirestoreUploadsEnabledUseCase @Inject constructor(
    private val session: UserSessionRepository,
    private val applyCollectionSettings: ApplyCollectionSettingsUseCase,
) {
    suspend operator fun invoke(enabled: Boolean) {
        session.setFirestoreUploadsEnabled(enabled)
        applyCollectionSettings()
    }
}
