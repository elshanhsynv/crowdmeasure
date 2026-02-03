package com.example.crowdmeasure.data.repo

import com.example.crowdmeasure.data.prefs.AppPreferences
import com.example.crowdmeasure.domain.repo.AppSettings
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import kotlinx.coroutines.flow.Flow

class UserSessionRepositoryImpl(
    private val prefs: AppPreferences
) : UserSessionRepository {

    override val settings: Flow<AppSettings> = prefs.settings

    override suspend fun setConsentAccepted(accepted: Boolean) = prefs.setConsentAccepted(accepted)
    override suspend fun setCollectionEnabled(enabled: Boolean) = prefs.setCollectionEnabled(enabled)
    override suspend fun setEndpointUrl(url: String) = prefs.setEndpointUrl(url)
    override suspend fun setCollectOnlyWifi(enabled: Boolean) = prefs.setCollectOnlyWifi(enabled)
    override suspend fun setAutoRun(enabled: Boolean, intervalHours: Int) = prefs.setAutoRun(enabled, intervalHours)
    override suspend fun setRetentionDays(days: Int) = prefs.setRetentionDays(days)
    override suspend fun setConsentGateDismissed(enabled: Boolean) = prefs.setConsentGateDismissed(enabled)
}