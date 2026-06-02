package com.example.crowdmeasure.workers

import android.os.DeadObjectException
import android.os.RemoteException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDiskIOException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.util.concurrent.TimeoutException

internal object WorkRetryClassifier {
    private const val MAX_TOTAL_ATTEMPTS = 5
    private val transientHttpStatuses = setOf(408, 429, 500, 502, 503, 504)

    private val transientFirestoreCodes = setOf(
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.INTERNAL,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
        FirebaseFirestoreException.Code.UNAVAILABLE
    )

    fun shouldRetry(error: Throwable, runAttemptCount: Int): Boolean {
        return shouldRetryAutoRun(error, runAttemptCount)
    }

    fun shouldRetryAutoRun(error: Throwable, runAttemptCount: Int): Boolean {
        if (error is CancellationException) throw error
        if (runAttemptCount >= MAX_TOTAL_ATTEMPTS) return false
        return error.causeChain().any(::isAutoRunTransient)
    }

    fun shouldRetryUpload(error: Throwable, runAttemptCount: Int): Boolean {
        if (error is CancellationException) throw error
        if (runAttemptCount >= MAX_TOTAL_ATTEMPTS) return false
        return error.causeChain().any(::isUploadTransient)
    }

    private fun isAutoRunTransient(error: Throwable): Boolean =
        when (error) {
            is IOException -> true
            is SQLiteCantOpenDatabaseException -> true
            is SQLiteDatabaseLockedException -> true
            is SQLiteDiskIOException -> true
            is DeadObjectException -> true
            is RemoteException -> true
            is TimeoutCancellationException -> true
            is TimeoutException -> true
            else -> false
        }

    private fun isUploadTransient(error: Throwable): Boolean =
        when (error) {
            is IOException -> true
            is FirebaseNetworkException -> true
            is FirebaseFirestoreException -> error.code in transientFirestoreCodes
            else -> error.httpStatusCode() in transientHttpStatuses
        }

    private fun Throwable.httpStatusCode(): Int? {
        val reflectedCode = runCatching {
            javaClass.methods
                .firstOrNull { it.name == "code" && it.parameterTypes.isEmpty() }
                ?.invoke(this) as? Int
        }.getOrNull()
        if (reflectedCode != null) return reflectedCode

        return Regex("""\bHTTP\s+(\d{3})\b|\bstatus(?:Code)?[=: ]+(\d{3})\b""", RegexOption.IGNORE_CASE)
            .find(message.orEmpty())
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotBlank() }
            ?.toIntOrNull()
    }

    private fun Throwable.causeChain(): Sequence<Throwable> = sequence {
        var current: Throwable? = this@causeChain
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            yield(current)
            current = current.cause?.takeUnless { it === current }
            depth++
        }
    }

    private const val MAX_CAUSE_DEPTH = 8
}
