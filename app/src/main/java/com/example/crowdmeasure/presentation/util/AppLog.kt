package com.example.crowdmeasure.presentation.util

import android.util.Log

object AppLog {
    private const val PREFIX = "CrowdMeasure"

    fun d(tag: String, msg: String) = Log.d("$PREFIX-$tag", msg)
    fun i(tag: String, msg: String) = Log.i("$PREFIX-$tag", msg)
    fun w(tag: String, msg: String, t: Throwable? = null) = Log.w("$PREFIX-$tag", msg, t)
    fun e(tag: String, msg: String, t: Throwable? = null) = Log.e("$PREFIX-$tag", msg, t)
}
