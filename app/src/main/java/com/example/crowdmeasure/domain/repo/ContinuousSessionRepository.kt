package com.example.crowdmeasure.domain.repo

import com.example.crowdmeasure.domain.model.ArmedContinuousSession
import com.example.crowdmeasure.domain.model.ContinuousSessionStatus
import kotlinx.coroutines.flow.Flow

interface ContinuousSessionRepository {
    fun observeStatus(): Flow<ContinuousSessionStatus>
    fun observeArmed(): Flow<ArmedContinuousSession?>
    suspend fun armedSession(): ArmedContinuousSession?

    /**
     * Starts a session that ends after [durationMinutes] unless it is stopped first, waiting
     * [gapSeconds] between measurements. Does nothing if one is running.
     */
    suspend fun start(durationMinutes: Int, gapSeconds: Int)
    suspend fun stop()

    suspend fun arm(durationMinutes: Int, gapSeconds: Int, afterMeasurementId: String?)
    suspend fun disarm()
}
