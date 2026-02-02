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
class MaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val measurementRepo: MeasurementRepository,
    private val sessionRepo: UserSessionRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = sessionRepo.settings.first()
        val cutoff = System.currentTimeMillis() - settings.retentionDays.toLong() * 24L * 60L * 60L * 1000L
        measurementRepo.deleteOlderThan(cutoff)
        return Result.success()
    }
}