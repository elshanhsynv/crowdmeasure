package com.example.crowdmeasure.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkDownloader @Inject constructor(
    @ApplicationContext context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val updateDir = File(context.cacheDir, "updates")

    fun download(metadata: UpdateMetadata): File {
        updateDir.mkdirs()
        val destination = File(updateDir, "crowdmeasure-v${metadata.versionCode}.apk")
        val request = Request.Builder().url(metadata.apkUrl).build()

        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "APK download failed: HTTP ${response.code}" }
            response.body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        return destination
    }
}
