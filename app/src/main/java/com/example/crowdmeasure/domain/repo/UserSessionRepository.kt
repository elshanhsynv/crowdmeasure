package com.example.crowdmeasure.domain.repo

import kotlinx.coroutines.flow.Flow

data class AppSettings(
//    val consentAccepted: Boolean,
//    val collectionEnabled: Boolean,

    val consentVersion: Int,
    val endpointUrl: String,
    val collectOnlyWifi: Boolean,
    val autoRunEnabled: Boolean,
    val autoRunIntervalMinutes: Int,
    val retentionDays: Int,
    val installId: String,
    val consentGateDismissed: Boolean,
    val firestoreUploadsEnabled: Boolean

)

interface UserSessionRepository {
    val settings: Flow<AppSettings>

//    suspend fun setConsentAccepted(accepted: Boolean)
//    suspend fun setCollectionEnabled(enabled: Boolean)
    suspend fun setEndpointUrl(url: String)
    suspend fun setCollectOnlyWifi(enabled: Boolean)
    suspend fun setAutoRun(enabled: Boolean, intervalMinutes: Int)
    suspend fun setRetentionDays(days: Int)
    suspend fun setConsentGateDismissed(enabled: Boolean)
    suspend fun setFirestoreUploadsEnabled(enabled: Boolean)
}