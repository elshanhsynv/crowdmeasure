package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

class SetEndpointUrlUseCase @Inject constructor(
    private val session: UserSessionRepository
) {
    suspend operator fun invoke(url: String) = session.setEndpointUrl(url)
}