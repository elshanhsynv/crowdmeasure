package com.example.crowdmeasure.update

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.OutputStream
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

        try {
            okHttpClient.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "APK download failed: HTTP ${response.code}" }
                val declaredLength = response.body.contentLength()
                check(declaredLength <= MAX_APK_BYTES) { "APK is larger than $MAX_APK_BYTES bytes" }
                val written = response.body.byteStream().use { input ->
                    partial.outputStream().use { output -> input.copyLimited(output, MAX_APK_BYTES) }
                }
                check(declaredLength < 0 || written == declaredLength) {
                    "APK download was incomplete ($written of $declaredLength bytes)"
                }
            }
            check(partial.renameTo(destination)) { "Could not save downloaded APK" }
        } finally {
            partial.delete()
        }
        return destination
    }

    private fun InputStream.copyLimited(output: OutputStream, limit: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) return total
            total += read
            check(total <= limit) { "APK is larger than $limit bytes" }
            output.write(buffer, 0, read)
        }
    }

    private companion object {
        const val MAX_APK_BYTES = 200L * 1024 * 1024
    }
}
