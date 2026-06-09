package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SetCollectOnlyWifiUseCase @Inject constructor(
    private val session: UserSessionRepository,
    private val background: BackgroundCollectionClient,
) {
    suspend operator fun invoke(enabled: Boolean) {
        session.setCollectOnlyWifi(enabled)
        val settings = session.settings.first()
        if (settings.autoRunEnabled) {
            background.enable(settings.autoRunIntervalMinutes.toLong().coerceAtLeast(20L), enabled)
        }
    }
}
