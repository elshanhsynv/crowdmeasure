package com.yourcompany.crowdmeasure.sdk.background.internal

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionClient
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionSettings
import com.yourcompany.crowdmeasure.sdk.background.BackgroundCollectionStatus
import com.yourcompany.crowdmeasure.sdk.background.BackgroundError
import com.yourcompany.crowdmeasure.sdk.background.BackgroundResult
import com.yourcompany.crowdmeasure.sdk.background.BackgroundWorkState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

internal object BackgroundWorkNames {
    const val PREFIX = "com.yourcompany.crowdmeasure.sdk.background"
    const val PERIODIC = "$PREFIX.measurement.periodic"
    const val RUN_NOW = "$PREFIX.measurement.immediate"
    const val CLEANUP = "$PREFIX.retention.daily"
    const val TAG = "$PREFIX.owned"
}

internal class BackgroundClientImpl(context: Context) : BackgroundCollectionClient {
    private val store = BackgroundStore(context)
    private val workManager = WorkManager.getInstance(context)

    override suspend fun enable(intervalMinutes: Long, wifiOnly: Boolean): BackgroundResult<Unit> {
        if (!isValidBackgroundInterval(intervalMinutes)) {
            return BackgroundResult.Failure(BackgroundError.InvalidInterval(intervalMinutes))
        }
        return scheduleSafely {
            val settings = BackgroundCollectionSettings(true, intervalMinutes, wifiOnly)
            store.setSettings(settings)
            schedule(settings)
        }
    }

    override suspend fun disable(): BackgroundResult<Unit> = scheduleSafely {
        val current = store.settings.first()
        store.setSettings(current.copy(enabled = false))
        cancelAll()
    }

    override suspend fun enqueueRunNow(): BackgroundResult<Unit> {
        val settings = store.settings.first()
        if (!settings.enabled) return BackgroundResult.Failure(BackgroundError.NotEnabled)
        return scheduleSafely {
            val request = OneTimeWorkRequestBuilder<MeasurementWorker>()
                .setConstraints(networkConstraints(settings.wifiOnly, false))
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .addTag(BackgroundWorkNames.TAG)
                .build()
            workManager.enqueueUniqueWork(BackgroundWorkNames.RUN_NOW, ExistingWorkPolicy.KEEP, request)
        }
    }

    override suspend fun reschedule(): BackgroundResult<Unit> = scheduleSafely {
        val settings = store.settings.first()
        if (settings.enabled) schedule(settings) else cancelAll()
    }

    override fun observeSettings() = store.settings

    override fun observeStatus() = combine(
        store.settings,
        store.lastRun,
        workManager.getWorkInfosForUniqueWorkFlow(BackgroundWorkNames.PERIODIC)
            .map { it.firstOrNull()?.state },
    ) { settings, lastRun, workState ->
        BackgroundCollectionStatus(
            settings = settings,
            workState = if (!settings.enabled) BackgroundWorkState.DISABLED else mapWorkState(workState?.name),
            lastRun = lastRun,
        )
    }

    private fun schedule(settings: BackgroundCollectionSettings) {
        val flex = (settings.intervalMinutes / 3).coerceAtLeast(5)
        val periodic = PeriodicWorkRequestBuilder<MeasurementWorker>(
            settings.intervalMinutes, TimeUnit.MINUTES, flex, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints(settings.wifiOnly, true))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .addTag(BackgroundWorkNames.TAG)
            .build()
        val cleanup = PeriodicWorkRequestBuilder<RetentionWorker>(24, TimeUnit.HOURS, 6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .addTag(BackgroundWorkNames.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            BackgroundWorkNames.PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, periodic
        )
        workManager.enqueueUniquePeriodicWork(
            BackgroundWorkNames.CLEANUP, ExistingPeriodicWorkPolicy.UPDATE, cleanup
        )
    }

    private fun cancelAll() {
        workManager.cancelUniqueWork(BackgroundWorkNames.PERIODIC)
        workManager.cancelUniqueWork(BackgroundWorkNames.RUN_NOW)
        workManager.cancelUniqueWork(BackgroundWorkNames.CLEANUP)
    }

    private fun networkConstraints(wifiOnly: Boolean, batteryNotLow: Boolean) =
        Constraints.Builder()
            .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(batteryNotLow)
            .build()

    private inline fun scheduleSafely(block: () -> Unit): BackgroundResult<Unit> =
        runCatching(block).fold(
            onSuccess = { BackgroundResult.Success(Unit) },
            onFailure = { BackgroundResult.Failure(BackgroundError.SchedulingFailed(it)) },
        )

}
