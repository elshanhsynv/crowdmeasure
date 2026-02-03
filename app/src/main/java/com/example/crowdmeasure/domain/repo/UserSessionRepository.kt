package com.example.crowdmeasure.domain.repo

import kotlinx.coroutines.flow.Flow

data class AppSettings(
    val consentAccepted: Boolean,
    val consentVersion: Int,
    val collectionEnabled: Boolean,
    val endpointUrl: String,
    val collectOnlyWifi: Boolean,
    val autoRunEnabled: Boolean,
    val autoRunIntervalHours: Int,
    val retentionDays: Int,
    val installId: String,
    val consentGateDismissed: Boolean,
)

interface UserSessionRepository {
    val settings: Flow<AppSettings>
    suspend fun setConsentAccepted(accepted: Boolean)
    suspend fun setCollectionEnabled(enabled: Boolean)
    suspend fun setEndpointUrl(url: String)
    suspend fun setCollectOnlyWifi(enabled: Boolean)
    suspend fun setAutoRun(enabled: Boolean, intervalHours: Int)
    suspend fun setRetentionDays(days: Int)

    suspend fun setConsentGateDismissed(enabled: Boolean)
}