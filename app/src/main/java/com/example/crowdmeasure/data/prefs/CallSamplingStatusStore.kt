package com.example.crowdmeasure.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallSamplingStatusStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    data class Status(
        val lastMissedAtUtcMs: Long = 0L,
        val lastMissedReason: String? = null
    )

    val status: Flow<Status> = context.dataStore.data.map { prefs ->
        Status(
            lastMissedAtUtcMs = prefs[DataStoreKeys.CALL_SAMPLING_LAST_MISSED_AT_UTC_MS] ?: 0L,
            lastMissedReason = prefs[DataStoreKeys.CALL_SAMPLING_LAST_MISSED_REASON]
        )
    }

    suspend fun recordMissedStart(reason: String) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.CALL_SAMPLING_LAST_MISSED_AT_UTC_MS] = System.currentTimeMillis()
            prefs[DataStoreKeys.CALL_SAMPLING_LAST_MISSED_REASON] = reason
        }
    }
}
