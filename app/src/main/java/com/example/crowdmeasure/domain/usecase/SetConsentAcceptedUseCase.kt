package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

class SetConsentAcceptedUseCase @Inject constructor(
    private val session: UserSessionRepository
) {
    suspend operator fun invoke(accepted: Boolean) {
        session.setConsentAccepted(accepted)
        if (!accepted) session.setCollectionEnabled(false)
    }
}