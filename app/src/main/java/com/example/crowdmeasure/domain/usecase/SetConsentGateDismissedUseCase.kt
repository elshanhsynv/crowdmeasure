package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

class SetConsentGateDismissedUseCase @Inject constructor(
    private val repository: UserSessionRepository
) {
    suspend operator fun invoke(dismissed: Boolean) {
        repository.setConsentGateDismissed(dismissed)
    }
}