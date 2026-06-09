package com.yourcompany.crowdmeasure.sdk.upload.internal

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.yourcompany.crowdmeasure.sdk.upload.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

private val Context.uploadDataStore by preferencesDataStore("crowdmeasure_sdk_upload")

internal class DefaultInstallationIdProvider(private val context: Context) : InstallationIdProvider {
    override suspend fun getInstallationId(): String {
        var id = context.uploadDataStore.data.map { it[Keys.installId] }.firstOrNull()
        if (id.isNullOrBlank()) {
            id = UUID.randomUUID().toString()
            context.uploadDataStore.edit { it[Keys.installId] = id }
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

internal class UploadStore(private val context: Context) {
    val settings = context.uploadDataStore.data.map {
        MeasurementUploadSettings(
            it[Keys.enabled] ?: false,
            it[Keys.interval] ?: CrowdMeasureUploads.DEFAULT_INTERVAL_MINUTES,
            it[Keys.wifiOnly] ?: true,
        )
    }
    val lastRun = context.uploadDataStore.data.map {
        val time = it[Keys.completedAt] ?: return@map null
        UploadRun(
            time,
            runCatching { UploadRunOutcome.valueOf(it[Keys.outcome].orEmpty()) }.getOrDefault(UploadRunOutcome.FAILURE),
            runCatching { UploadRunCode.valueOf(it[Keys.code].orEmpty()) }.getOrDefault(UploadRunCode.UNEXPECTED_ERROR),
            it[Keys.uploadedCount] ?: 0,
            it[Keys.error]?.takeIf(String::isNotBlank),
        )
    }
    suspend fun setSettings(value: MeasurementUploadSettings) = context.uploadDataStore.edit {
        it[Keys.enabled] = value.enabled
        it[Keys.interval] = value.intervalMinutes
        it[Keys.wifiOnly] = value.wifiOnly
    }
    suspend fun record(run: UploadRun) = context.uploadDataStore.edit {
        it[Keys.completedAt] = run.completedAtUtcMs
        it[Keys.outcome] = run.outcome.name
        it[Keys.code] = run.code.name
        it[Keys.uploadedCount] = run.uploadedCount
        it[Keys.error] = run.errorMessage.orEmpty()
    }
}
