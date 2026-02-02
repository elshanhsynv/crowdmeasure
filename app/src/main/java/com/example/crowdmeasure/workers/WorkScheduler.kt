package com.example.crowdmeasure.workers

import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    companion object {
        private const val AUTO_RUN_NAME = "auto_run_measurement"
        private const val MAINTENANCE_NAME = "maintenance_cleanup"
    }

    fun scheduleMaintenanceDaily() {
        val req = PeriodicWorkRequestBuilder<MaintenanceWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            MAINTENANCE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun scheduleAutoRun(intervalHours: Int, wifiOnly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .apply { if (wifiOnly) setRequiredNetworkType(NetworkType.UNMETERED) else setRequiredNetworkType(NetworkType.CONNECTED) }
            .build()

        val req = PeriodicWorkRequestBuilder<AutoRunWorker>(intervalHours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            AUTO_RUN_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    fun cancelAutoRun() {
        workManager.cancelUniqueWork(AUTO_RUN_NAME)
    }
}