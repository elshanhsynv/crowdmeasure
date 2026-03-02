package com.example.crowdmeasure.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {
    fun shareJson(context: Context, uri: Uri, chooserTitle: String = "Share export JSON") {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("crowdmeasure_export.json", uri)
        }

        context.startActivity(Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}