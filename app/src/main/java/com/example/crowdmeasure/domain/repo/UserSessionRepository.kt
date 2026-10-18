package com.example.crowdmeasure.domain.repo

import kotlinx.coroutines.flow.Flow

data class AppSettings(
    /** The consent version the app currently requires. */
    val consentVersion: Int,
    /** The consent version the user last accepted, or 0 if never. */
    val consentAcceptedVersion: Int,
    val consentAcceptedAtUtcMs: Long?,
    val endpointUrl: String,
    val collectOnlyWifi: Boolean,
    val autoRunEnabled: Boolean,
    val autoRunIntervalMinutes: Int,
    val retentionDays: Int,
    val installId: String,
    /** True when the user has accepted the current [consentVersion]; collection is off otherwise. */
    val consentGateDismissed: Boolean,
    val firestoreUploadsEnabled: Boolean,
    val callSamplingEnabled: Boolean,
    val voipCallSamplingEnabled: Boolean
)

interface UserSessionRepository {
    val settings: Flow<AppSettings>
    suspend fun setAutoRun(enabled: Boolean, intervalMinutes: Int)
    suspend fun acceptConsent(atUtcMs: Long)
    suspend fun setFirestoreUploadsEnabled(enabled: Boolean)
    suspend fun setCallSamplingEnabled(enabled: Boolean)
    suspend fun setVoipCallSamplingEnabled(enabled: Boolean)
}
