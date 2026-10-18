package com.example.crowdmeasure.update

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val updateRepository: UpdateRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return updateRepository.checkForUpdate(notify = true).fold(
            onSuccess = { Result.success() },
            // Only network errors are worth retrying; bad metadata waits for the next daily run.
            onFailure = { if (it is IOException) Result.retry() else Result.failure() }
        )
    }
}
