package com.example.crowdmeasure.domain.repo

import kotlinx.coroutines.flow.Flow

data class AppSettings(
    val consentVersion: Int,
    val endpointUrl: String,
    val collectOnlyWifi: Boolean,
    val autoRunEnabled: Boolean,
    val autoRunIntervalMinutes: Int,
    val retentionDays: Int,
    val installId: String,
    val consentGateDismissed: Boolean,
    val firestoreUploadsEnabled: Boolean,
    val callSamplingEnabled: Boolean,
    val whatsappCallSamplingEnabled: Boolean,
    val voipCallSamplingEnabled: Boolean
)

interface UserSessionRepository {
    val settings: Flow<AppSettings>
    suspend fun setEndpointUrl(url: String)
    suspend fun setCollectOnlyWifi(enabled: Boolean)
    suspend fun setAutoRun(enabled: Boolean, intervalMinutes: Int)
    suspend fun setRetentionDays(days: Int)
    suspend fun setConsentGateDismissed(enabled: Boolean)
    suspend fun setFirestoreUploadsEnabled(enabled: Boolean)
    suspend fun setCallSamplingEnabled(enabled: Boolean)
    suspend fun setWhatsappCallSamplingEnabled(enabled: Boolean)
    suspend fun setVoipCallSamplingEnabled(enabled: Boolean)
}
