package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

class SetConsentAcceptedUseCase @Inject constructor(
    private val session: UserSessionRepository
) {
    suspend operator fun invoke(accepted: Boolean) {
        session.setConsentAccepted(accepted)
        // If consent is revoked, collection must be disabled too (privacy rule).
        if (!accepted) session.setCollectionEnabled(false)
    }
}