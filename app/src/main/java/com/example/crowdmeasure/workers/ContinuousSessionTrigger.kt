package com.example.crowdmeasure.workers

import com.example.crowdmeasure.domain.model.shouldStartArmedSession
import com.example.crowdmeasure.domain.repo.ContinuousSessionRepository
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.presentation.util.AppLog
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Starts an armed continuous session when another measurement finishes. Manual and scheduled
 * background measurements are both saved through the SDK, so both reach this observer.
 *
 * Meant to run for the life of the process, started from the application.
 */
class ContinuousSessionTrigger @Inject constructor(
    private val measurements: MeasurementRepository,
    private val sessions: ContinuousSessionRepository,
) {
    suspend fun run() {
        measurements.observeLastMeasurement()
            .map { it?.meta?.measurementId }
            .distinctUntilChanged()
            .collect { latestId ->
                val armed = sessions.armedSession() ?: return@collect
                if (!shouldStartArmedSession(armed.afterMeasurementId, latestId)) return@collect

                sessions.disarm()
                if (sessions.observeStatus().first().running) {
                    AppLog.i(TAG, "Armed session dropped: a session is already running.")
                    return@collect
                }
                AppLog.i(TAG, "Starting armed ${armed.durationMinutes}-minute session after measurement $latestId.")
                sessions.start(armed.durationMinutes, armed.gapSeconds)
            }
    }

    companion object {
        private const val TAG = "ContinuousSessionTrigger"
    }
}
