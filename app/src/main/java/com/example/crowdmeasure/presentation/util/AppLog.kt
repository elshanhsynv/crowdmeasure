package com.example.crowdmeasure.presentation.util

import android.util.Log
import timber.log.Timber

object AppLog {
    private const val PREFIX = "CrowdMeasure"

    fun d(tag: String, msg: String) = Timber.tag("$PREFIX-$tag").d(msg)
    fun i(tag: String, msg: String) = Timber.tag("$PREFIX-$tag").i(msg)
    fun w(tag: String, msg: String, t: Throwable? = null) = Timber.tag("$PREFIX-$tag").w(t, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = Timber.tag("$PREFIX-$tag").e(t, msg)
}
