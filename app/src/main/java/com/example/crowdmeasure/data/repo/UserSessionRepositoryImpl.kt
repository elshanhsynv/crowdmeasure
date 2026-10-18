package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.flow.Flow

class UserSessionRepositoryImpl(
    private val prefs: AppPreferences
) : UserSessionRepository {

    override val settings: Flow<AppSettings> = prefs.settings
    override suspend fun setAutoRun(enabled: Boolean, intervalMinutes: Int) = prefs.setAutoRun(enabled, intervalMinutes)
    override suspend fun acceptConsent(atUtcMs: Long) = prefs.acceptConsent(atUtcMs)
    override suspend fun setFirestoreUploadsEnabled(enabled: Boolean) {
        prefs.setFirestoreUploadsEnabled(enabled)
    }

    override suspend fun setCallSamplingEnabled(enabled: Boolean) {
        prefs.setCallSamplingEnabled(enabled)
    }

    override suspend fun setVoipCallSamplingEnabled(enabled: Boolean) {
        prefs.setVoipCallSamplingEnabled(enabled)
    }
}
