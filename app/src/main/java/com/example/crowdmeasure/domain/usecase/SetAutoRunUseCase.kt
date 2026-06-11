package com.example.crowdmeasure.domain.usecase

import com.example.crowdmeasure.domain.repo.UserSessionRepository
import com.crowdmeasure.sdk.background.BackgroundCollectionClient
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SetAutoRunUseCase @Inject constructor(
    private val session: UserSessionRepository,
    private val background: BackgroundCollectionClient,
) {
    suspend operator fun invoke(enabled: Boolean, intervalMinutes: Int) {
        session.setAutoRun(enabled, intervalMinutes)
        if (enabled) {
            background.enable(
                intervalMinutes = intervalMinutes.toLong().coerceAtLeast(20L),
                wifiOnly = session.settings.first().collectOnlyWifi,
            )
        } else {
            background.disable()
        }
    }
}
