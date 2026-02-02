package com.example.crowdmeasure.data.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val CONSENT_ACCEPTED = booleanPreferencesKey("consent_accepted")
    val COLLECTION_ENABLED = booleanPreferencesKey("collection_enabled")
    val ENDPOINT_URL = stringPreferencesKey("endpoint_url")
    val COLLECT_ONLY_WIFI = booleanPreferencesKey("collect_only_wifi")
    val AUTO_RUN_ENABLED = booleanPreferencesKey("auto_run_enabled")
    val AUTO_RUN_INTERVAL_HOURS = intPreferencesKey("auto_run_interval_hours")
    val RETENTION_DAYS = intPreferencesKey("retention_days")
    val INSTALL_ID = stringPreferencesKey("install_id")
}