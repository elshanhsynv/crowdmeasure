package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

/** Records acceptance of the current consent version, then starts the features the user has on. */
class AcceptConsentUseCase @Inject constructor(
    private val repository: UserSessionRepository,
    private val applyCollectionSettings: ApplyCollectionSettingsUseCase,
) {
    suspend operator fun invoke() {
        repository.acceptConsent(System.currentTimeMillis())
        applyCollectionSettings()
    }
}
