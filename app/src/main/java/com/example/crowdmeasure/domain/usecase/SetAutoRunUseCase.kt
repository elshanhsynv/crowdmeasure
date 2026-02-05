package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

class SetAutoRunUseCase @Inject constructor(
    private val session: UserSessionRepository
) {
    suspend operator fun invoke(enabled: Boolean, intervalMinutes: Int) {
        session.setAutoRun(enabled, intervalMinutes)
    }
}
