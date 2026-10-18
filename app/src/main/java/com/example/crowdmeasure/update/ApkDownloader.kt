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
        // Only one update is ever in flight, so drop APKs and partial downloads left by earlier attempts.
        updateDir.listFiles()?.forEach(File::delete)
        updateDir.mkdirs()
        val destination = File(updateDir, "crowdmeasure-v${metadata.versionCode}.apk")
        val partial = File(updateDir, "${destination.name}.part")
        val request = Request.Builder().url(metadata.apkUrl).build()

        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "APK download failed: HTTP ${response.code}" }
            response.body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        check(partial.renameTo(destination)) { "Could not save downloaded APK" }
        return destination
    }
}
