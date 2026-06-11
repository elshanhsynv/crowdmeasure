package com.crowdmeasure.sdk.upload.internal

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.crowdmeasure.sdk.upload.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

internal class DefaultInstallationIdProvider(context: Context, preferencesName: String) : InstallationIdProvider {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(preferencesName)
    }
    override suspend fun getInstallationId(): String {
        var id = dataStore.data.map { it[Keys.installId] }.firstOrNull()
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            dataStore.edit { it[Keys.installId] = id }
        }
        return id
    }
}

internal object Keys {
    val enabled = booleanPreferencesKey("enabled")
    val interval = longPreferencesKey("interval")
    val wifiOnly = booleanPreferencesKey("wifi_only")
    val installId = stringPreferencesKey("installation_id")
    val completedAt = longPreferencesKey("last_completed_at")
    val outcome = stringPreferencesKey("last_outcome")
    val code = stringPreferencesKey("last_code")
    val uploadedCount = intPreferencesKey("last_uploaded_count")
    val error = stringPreferencesKey("last_error")
}

internal class UploadStore(context: Context, private val config: MeasurementUploadConfig) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(config.preferencesName)
    }
    val settings = dataStore.data.map {
        MeasurementUploadSettings(
            it[Keys.enabled] ?: false,
            it[Keys.interval] ?: config.defaultIntervalMinutes,
            it[Keys.wifiOnly] ?: config.defaultWifiOnly,
        )
    }
    val lastRun = dataStore.data.map {
        val time = it[Keys.completedAt] ?: return@map null
        UploadRun(
            time,
            runCatching { UploadRunOutcome.valueOf(it[Keys.outcome].orEmpty()) }.getOrDefault(UploadRunOutcome.FAILURE),
            runCatching { UploadRunCode.valueOf(it[Keys.code].orEmpty()) }.getOrDefault(UploadRunCode.UNEXPECTED_ERROR),
            it[Keys.uploadedCount] ?: 0,
            it[Keys.error]?.takeIf(String::isNotBlank),
        )
    }
    suspend fun setSettings(value: MeasurementUploadSettings) = dataStore.edit {
        it[Keys.enabled] = value.enabled
        it[Keys.interval] = value.intervalMinutes
        it[Keys.wifiOnly] = value.wifiOnly
    }
    suspend fun record(run: UploadRun) = dataStore.edit {
        it[Keys.completedAt] = run.completedAtUtcMs
        it[Keys.outcome] = run.outcome.name
        it[Keys.code] = run.code.name
        it[Keys.uploadedCount] = run.uploadedCount
        it[Keys.error] = run.errorMessage.orEmpty()
    }
}
