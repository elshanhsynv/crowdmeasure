package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import javax.inject.Inject

class SetCollectOnlyWifiUseCase @Inject constructor(
    private val session: UserSessionRepository
) {
    suspend operator fun invoke(enabled: Boolean) = session.setCollectOnlyWifi(enabled)
}