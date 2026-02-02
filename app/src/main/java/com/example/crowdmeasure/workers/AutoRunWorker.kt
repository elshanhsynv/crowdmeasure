package com.example.crowdmeasure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.crowdmeasure.domain.repo.MeasurementRepository
import com.example.crowdmeasure.domain.repo.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class AutoRunWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sessionRepo: UserSessionRepository,
    private val measurementRepo: MeasurementRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = sessionRepo.settings.first()

        // Respect privacy rule: no background unless auto-run enabled AND consent accepted and collection enabled.
        if (!settings.autoRunEnabled || !settings.consentAccepted || !settings.collectionEnabled) {
            return Result.success()
        }

        val result = measurementRepo.runSingleMeasurement()
        return result.fold(
            onSuccess = {
                measurementRepo.insert(it)
                Result.success()
            },
            onFailure = { Result.retry() }
        )
    }
}