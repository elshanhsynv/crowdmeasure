package com.example.crowdmeasure.data.prefs

import androidx.datastore.preferences.core.*

object DataStoreKeys {
    val CONSENT_ACCEPTED = booleanPreferencesKey("consent_accepted")
    val COLLECTION_ENABLED = booleanPreferencesKey("collection_enabled")
    val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
    val COLLECT_ONLY_WIFI = booleanPreferencesKey("collect_only_wifi")
    val AUTO_RUN_ENABLED = booleanPreferencesKey("auto_run_enabled")
    val AUTO_RUN_INTERVAL_MINUTES = intPreferencesKey("auto_run_interval_minutes")
    val RETENTION_DAYS = intPreferencesKey("retention_days")
    val INSTALL_ID = stringPreferencesKey("install_id")
    val CONSENT_GATE_DISMISSED = booleanPreferencesKey("consent_gate_dismissed")
    val FIRESTORE_UPLOADS_ENABLED = booleanPreferencesKey("firestore_uploads_enabled")
    val AUTORUN_LAST_START_UTC_MS = longPreferencesKey("autorun_last_start_utc_ms")
    val AUTORUN_LAST_END_UTC_MS = longPreferencesKey("autorun_last_end_utc_ms")
    val AUTORUN_LAST_RESULT = stringPreferencesKey("autorun_last_result")
    val AUTORUN_LAST_CODE = stringPreferencesKey("autorun_last_code")
    val AUTORUN_LAST_ERROR = stringPreferencesKey("autorun_last_error")
    val AUTORUN_LAST_UPLOADED_COUNT = intPreferencesKey("autorun_last_uploaded_count")
    val AUTORUN_LAST_MEASUREMENT_ID = stringPreferencesKey("autorun_last_measurement_id")
    val AUTORUN_LAST_MEASUREMENT_TIMESTAMP_UTC_MS =
        longPreferencesKey("autorun_last_measurement_timestamp_utc_ms")
    val AUTORUN_LAST_SUCCESS_UTC_MS = longPreferencesKey("autorun_last_success_utc_ms")
    val AUTORUN_LAST_SCHEDULE_MINUTES = intPreferencesKey("autorun_last_schedule_minutes")
    val AUTORUN_LAST_SCHEDULE_WIFI_ONLY = booleanPreferencesKey("autorun_last_schedule_wifi_only")
    val UPLOAD_LAST_START_UTC_MS = longPreferencesKey("upload_last_start_utc_ms")
    val UPLOAD_LAST_END_UTC_MS = longPreferencesKey("upload_last_end_utc_ms")
    val UPLOAD_LAST_RESULT = stringPreferencesKey("upload_last_result")
    val UPLOAD_LAST_CODE = stringPreferencesKey("upload_last_code")
    val UPLOAD_LAST_UPLOADED_COUNT = intPreferencesKey("upload_last_uploaded_count")
    val UPLOAD_PENDING_COUNT = intPreferencesKey("upload_pending_count")
    val UPLOAD_FAILED_COUNT = intPreferencesKey("upload_failed_count")
    val UPLOAD_LAST_ERROR = stringPreferencesKey("upload_last_error")
    val UPLOAD_LAST_SUCCESS_UTC_MS = longPreferencesKey("upload_last_success_utc_ms")
}
