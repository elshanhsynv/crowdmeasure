package com.yourcompany.crowdmeasure.sdk.calls.internal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingError
import com.yourcompany.crowdmeasure.sdk.calls.CallSamplingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal class CallUploadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val runtime = CallsRuntime.get() ?: return Result.failure()
        val client = CallSamplingClientImpl(applicationContext)
        return when (val result = client.uploadPending()) {
            is CallSamplingResult.Success -> Result.success()
            is CallSamplingResult.Failure -> if (result.error is CallSamplingError.TransientFailure) Result.retry() else Result.failure()
        }
    }
}

internal class CallWorkRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (CallsRuntime.get() == null) return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                CallSamplingClientImpl(context.applicationContext).activateEnabledFeatures()
            } finally {
                pending.finish()
            }
        }
    }
}
