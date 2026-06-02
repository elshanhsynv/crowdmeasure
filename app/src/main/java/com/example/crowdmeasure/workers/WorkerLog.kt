// workers/WorkerLog.kt
package com.example.crowdmeasure.workers

import com.example.crowdmeasure.presentation.util.AppLog

internal object WorkerLog {
    fun i(tag: String, msg: String) = AppLog.i(tag, msg)
    fun w(tag: String, msg: String, t: Throwable? = null) = AppLog.w(tag, msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = AppLog.e(tag, msg, t)

    private fun redact(msg: String): String = msg
        .replace(Regex("measurementId=\\S+"), "measurementId=<redacted>")
        .replace(Regex("consent=\\S+"), "consent=<redacted>")
}
