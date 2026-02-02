package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class SetCollectionEnabledUseCase @Inject constructor(
    private val session: UserSessionRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        // Only allow enabling collection if consent is accepted.
        val current = session.settings.firstOrNull()
        val consentAccepted = current?.consentAccepted == true
        session.setCollectionEnabled(enabled && consentAccepted)
    }
}