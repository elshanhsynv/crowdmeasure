package com.example.crowdmeasure.workers

import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDiskIOException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import java.io.IOException

internal object WorkRetryClassifier {
    private const val MAX_RETRY_ATTEMPTS = 5

    private val transientFirestoreCodes = setOf(
        FirebaseFirestoreException.Code.ABORTED,
        FirebaseFirestoreException.Code.CANCELLED,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
        FirebaseFirestoreException.Code.INTERNAL,
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
        FirebaseFirestoreException.Code.UNAVAILABLE
    )

    fun shouldRetry(error: Throwable, runAttemptCount: Int): Boolean {
        if (error is CancellationException) throw error
        if (runAttemptCount >= MAX_RETRY_ATTEMPTS) return false
        return error.causeChain().any(::isTransient)
    }

    private fun isTransient(error: Throwable): Boolean =
        when (error) {
            is IOException -> true
            is SQLiteCantOpenDatabaseException -> true
            is SQLiteDatabaseLockedException -> true
            is SQLiteDiskIOException -> true
            is FirebaseNetworkException -> true
            is FirebaseFirestoreException -> error.code in transientFirestoreCodes
            else -> false
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
