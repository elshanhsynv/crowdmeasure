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
        const val DEFAULT_ENDPOINT = "https://www.google.com/"
        const val DEFAULT_RETENTION_DAYS = 7
        const val DEFAULT_AUTORUN_MINUTES = 20
        const val DEFAULT_COLLECT_ONLY_ON_WIFI = true
        const val DEFAULT_AUTO_RUN_ENABLED = true
        const val DEFAULT_FIRESTORE_UPLOADS_ENABLED = true
        const val DEFAULT_CALL_SAMPLING_ENABLED = true
        const val DEFAULT_VOIP_CALL_SAMPLING_ENABLED = true
        const val DEFAULT_WHATSAPP_CALL_SAMPLING_ENABLED = true
        const val DEFAULT_CONSENT_GATE_DISMISSED = false
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val installId = prefs[DataStoreKeys.INSTALL_ID] ?: ""
        val minutes = prefs[DataStoreKeys.AUTO_RUN_INTERVAL_MINUTES] ?: DEFAULT_AUTORUN_MINUTES

        AppSettings(
            consentVersion = CONSENT_VERSION,
            endpointUrl = prefs[DataStoreKeys.ENDPOINT_URL] ?: DEFAULT_ENDPOINT,
            collectOnlyWifi = prefs[DataStoreKeys.COLLECT_ONLY_WIFI] ?: DEFAULT_COLLECT_ONLY_ON_WIFI,
            autoRunEnabled = prefs[DataStoreKeys.AUTO_RUN_ENABLED] ?: DEFAULT_AUTO_RUN_ENABLED,
            autoRunIntervalMinutes = minutes.coerceIn(15, 7 * 24 * 60),
            retentionDays = prefs[DataStoreKeys.RETENTION_DAYS] ?: DEFAULT_RETENTION_DAYS,
            installId = installId,
            consentGateDismissed = prefs[DataStoreKeys.CONSENT_GATE_DISMISSED] ?: DEFAULT_CONSENT_GATE_DISMISSED,
            firestoreUploadsEnabled = prefs[DataStoreKeys.FIRESTORE_UPLOADS_ENABLED] ?: DEFAULT_FIRESTORE_UPLOADS_ENABLED,
            callSamplingEnabled = prefs[DataStoreKeys.CALL_SAMPLING_ENABLED] ?: DEFAULT_CALL_SAMPLING_ENABLED,
            // Retained for existing-data compatibility; WhatsApp detection is not available.
            whatsappCallSamplingEnabled = prefs[DataStoreKeys.WHATSAPP_CALL_SAMPLING_ENABLED] ?: DEFAULT_WHATSAPP_CALL_SAMPLING_ENABLED,
            voipCallSamplingEnabled = prefs[DataStoreKeys.VOIP_CALL_SAMPLING_ENABLED] ?: DEFAULT_VOIP_CALL_SAMPLING_ENABLED,
        )
    }

    val batteryOptimizationRecommendationDismissedUntil: Flow<Long> =
        context.dataStore.data.map { prefs ->
            prefs[DataStoreKeys.BATTERY_OPTIMIZATION_RECOMMENDATION_DISMISSED_UNTIL] ?: 0L
        }

    suspend fun ensureInstallId() {
        context.dataStore.edit { prefs ->
            val existing = prefs[DataStoreKeys.INSTALL_ID]
            if (existing.isNullOrBlank()) {
                prefs[DataStoreKeys.INSTALL_ID] = UUID.randomUUID().toString()
            }
        }
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

    suspend fun setFirestoreUploadsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.FIRESTORE_UPLOADS_ENABLED] = enabled }
    }

    suspend fun setCallSamplingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.CALL_SAMPLING_ENABLED] = enabled }
    }

    suspend fun setWhatsappCallSamplingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.WHATSAPP_CALL_SAMPLING_ENABLED] = enabled }
    }

    suspend fun setVoipCallSamplingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DataStoreKeys.VOIP_CALL_SAMPLING_ENABLED] = enabled }
    }

    suspend fun isSdkBackgroundMigrated(): Boolean =
        context.dataStore.data.first()[DataStoreKeys.SDK_BACKGROUND_MIGRATED] ?: false

    suspend fun markSdkBackgroundMigrated() {
        context.dataStore.edit { it[DataStoreKeys.SDK_BACKGROUND_MIGRATED] = true }
    }

    suspend fun isSdkUploadMigrated(): Boolean =
        context.dataStore.data.first()[DataStoreKeys.SDK_UPLOAD_MIGRATED] ?: false

    suspend fun markSdkUploadMigrated() {
        context.dataStore.edit { it[DataStoreKeys.SDK_UPLOAD_MIGRATED] = true }
    }

    suspend fun isSdkCallsMigrated(): Boolean =
        context.dataStore.data.first()[DataStoreKeys.SDK_CALLS_MIGRATED] ?: false

    suspend fun markSdkCallsMigrated() {
        context.dataStore.edit { it[DataStoreKeys.SDK_CALLS_MIGRATED] = true }
    }

    suspend fun installationId(): String {
        ensureInstallId()
        return settingsFirst().installId
    }

    suspend fun setBatteryOptimizationRecommendationDismissedUntil(timestampUtcMs: Long) {
        context.dataStore.edit {
            it[DataStoreKeys.BATTERY_OPTIMIZATION_RECOMMENDATION_DISMISSED_UNTIL] =
                timestampUtcMs.coerceAtLeast(0L)
        }
    }

    suspend fun lastNotifiedUpdateVersionCode(): Int =
        context.dataStore.data.first()[DataStoreKeys.UPDATE_LAST_NOTIFIED_VERSION_CODE] ?: 0

    suspend fun markUpdateVersionNotified(versionCode: Int) {
        context.dataStore.edit {
            it[DataStoreKeys.UPDATE_LAST_NOTIFIED_VERSION_CODE] = versionCode.coerceAtLeast(0)
        }
    }
}
