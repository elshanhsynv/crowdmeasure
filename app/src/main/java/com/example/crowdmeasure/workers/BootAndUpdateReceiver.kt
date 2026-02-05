package com.example.crowdmeasure.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootAndUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        WorkManagerInit.ensureInitialized(context)

        val req = OneTimeWorkRequestBuilder<WorkRescheduleWorker>()
            .setConstraints(Constraints.NONE)
            .addTag(WorkScheduler.TAG_RESCHEDULE)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WorkScheduler.RESCHEDULE_NAME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }
}
