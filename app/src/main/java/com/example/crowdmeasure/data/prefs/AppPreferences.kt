package com.example.crowdmeasure.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.crowdmeasure.domain.repo.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlinx.coroutines.flow.first

class AppPreferences(private val context: Context) {

    suspend fun settingsFirst(): AppSettings = settings.first()

    companion object {
        const val CONSENT_VERSION = 1
        const val DEFAULT_ENDPOINT = "https://google.com"
        const val DEFAULT_RETENTION_DAYS = 7
        const val DEFAULT_AUTORUN_MINUTES = 15
    }



    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val installId = prefs[DataStoreKeys.INSTALL_ID] ?: ""
        val minutes = prefs[DataStoreKeys.AUTO_RUN_INTERVAL_MINUTES] ?: DEFAULT_AUTORUN_MINUTES

        AppSettings(
            consentAccepted = prefs[DataStoreKeys.CONSENT_ACCEPTED] ?: false,
            consentVersion = CONSENT_VERSION,
            collectionEnabled = prefs[DataStoreKeys.COLLECTION_ENABLED] ?: false,
            endpointUrl = prefs[DataStoreKeys.ENDPOINT_URL] ?: DEFAULT_ENDPOINT,
            collectOnlyWifi = prefs[DataStoreKeys.COLLECT_ONLY_WIFI] ?: false,
            autoRunEnabled = prefs[DataStoreKeys.AUTO_RUN_ENABLED] ?: false,
            autoRunIntervalMinutes = minutes.coerceIn(15, 7 * 24 * 60),
            retentionDays = prefs[DataStoreKeys.RETENTION_DAYS] ?: DEFAULT_RETENTION_DAYS,
            installId = installId,
            consentGateDismissed = prefs[DataStoreKeys.CONSENT_GATE_DISMISSED] ?: false,
            firestoreUploadsEnabled = prefs[DataStoreKeys.FIRESTORE_UPLOADS_ENABLED] ?: false,
        )
    }

    suspend fun ensureInstallId() {
        context.dataStore.edit { prefs ->
            val existing = prefs[DataStoreKeys.INSTALL_ID]
            if (existing.isNullOrBlank()) {
                prefs[DataStoreKeys.INSTALL_ID] = UUID.randomUUID().toString()
            }
        }
    }

    suspend fun setConsentAccepted(accepted: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.CONSENT_ACCEPTED] = accepted }
    }

    suspend fun setCollectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.COLLECTION_ENABLED] = enabled }
    }

    suspend fun setEndpointUrl(url: String) {
        context.dataStore.edit { it[DataStoreKeys.ENDPOINT_URL] = url.trim() }
    }

    suspend fun setCollectOnlyWifi(enabled: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.COLLECT_ONLY_WIFI] = enabled }
    }

    suspend fun setAutoRun(enabled: Boolean, intervalMinutes: Int) {
        context.dataStore.edit {
            it[DataStoreKeys.AUTO_RUN_ENABLED] = enabled
            it[DataStoreKeys.AUTO_RUN_INTERVAL_MINUTES] = intervalMinutes.coerceIn(15, 7 * 24 * 60)
        }
    }


    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit { it[DataStoreKeys.RETENTION_DAYS] = days.coerceIn(1, 60) }
    }

    suspend fun setConsentGateDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.CONSENT_GATE_DISMISSED] = dismissed }
    }

    // Optional: when user deletes data, you may want gate to appear again
    suspend fun resetConsentGateDismissed() {
        context.dataStore.edit { it[DataStoreKeys.CONSENT_GATE_DISMISSED] = false }
    }

    suspend fun setFirestoreUploadsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.FIRESTORE_UPLOADS_ENABLED] = enabled }
    }
}