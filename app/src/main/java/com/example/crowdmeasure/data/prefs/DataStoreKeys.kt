package com.example.crowdmeasure.data.prefs

import androidx.datastore.preferences.core.*

object DataStoreKeys {
    val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
    val COLLECT_ONLY_WIFI = booleanPreferencesKey("collect_only_wifi")
    val AUTO_RUN_ENABLED = booleanPreferencesKey("auto_run_enabled")
    val AUTO_RUN_INTERVAL_MINUTES = intPreferencesKey("auto_run_interval_minutes")
    val SDK_BACKGROUND_MIGRATED = booleanPreferencesKey("sdk_background_migrated")
    val SDK_UPLOAD_MIGRATED = booleanPreferencesKey("sdk_upload_migrated")
    val SDK_CALLS_MIGRATED = booleanPreferencesKey("sdk_calls_migrated")
    val RETENTION_DAYS = intPreferencesKey("retention_days")
    val INSTALL_ID = stringPreferencesKey("install_id")
    val CONSENT_GATE_DISMISSED = booleanPreferencesKey("consent_gate_dismissed")
    val CONSENT_ACCEPTED_VERSION = intPreferencesKey("consent_accepted_version")
    val CONSENT_ACCEPTED_AT_UTC_MS = longPreferencesKey("consent_accepted_at_utc_ms")
    val FIRESTORE_UPLOADS_ENABLED = booleanPreferencesKey("firestore_uploads_enabled")
    val CALL_SAMPLING_ENABLED = booleanPreferencesKey("call_sampling_enabled")
    val VOIP_CALL_SAMPLING_ENABLED =
        booleanPreferencesKey("voip_call_sampling_enabled")
    val BATTERY_OPTIMIZATION_RECOMMENDATION_DISMISSED_UNTIL =
        longPreferencesKey("battery_optimization_recommendation_dismissed_until")
    val UPDATE_LAST_NOTIFIED_VERSION_CODE =
        intPreferencesKey("update_last_notified_version_code")
    val CONTINUOUS_ENDS_AT_UTC_MS = longPreferencesKey("continuous_ends_at_utc_ms")
    val CONTINUOUS_COLLECTED_COUNT = intPreferencesKey("continuous_collected_count")
    val CONTINUOUS_LAST_ERROR = stringPreferencesKey("continuous_last_error")
    val CONTINUOUS_ARMED_DURATION_MINUTES = intPreferencesKey("continuous_armed_duration_minutes")
    val CONTINUOUS_ARMED_GAP_SECONDS = intPreferencesKey("continuous_armed_gap_seconds")
    val CONTINUOUS_ARMED_AFTER_MEASUREMENT_ID =
        stringPreferencesKey("continuous_armed_after_measurement_id")
}
