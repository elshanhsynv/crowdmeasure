package com.example.crowdmeasure.data.measurement.collectors

import androidx.annotation.WorkerThread
import com.example.crowdmeasure.domain.model.PerfThroughputTestConfig
import kotlinx.coroutines.CancellationException
import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.blackholeSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.sqrt

data class PerfThroughputResult(
    val downMbps: Double? = null,
    val upMbps: Double? = null,
    val downP95Mbps: Double? = null,
    val downStdDevMbps: Double? = null,
    val upP95Mbps: Double? = null,
    val upStdDevMbps: Double? = null,
    val testPayloadBytes: Long? = null,
)

object PerfThroughputCollector {

    private const val READ_CHUNK_BYTES = 64L * 1024L
    private const val WRITE_CHUNK_BYTES = 64 * 1024

    /**
     * Measures approximate download/upload throughput.
     *
     * Download can work with a public static file URL.
     * Upload should only be enabled when you control the upload endpoint.
     */
    @WorkerThread
    fun run(
        okHttp: OkHttpClient,
        config: PerfThroughputTestConfig,
    ): PerfThroughputResult {
        if (!config.enabled) return PerfThroughputResult()

        val safeConfig = config.normalized()
        if (safeConfig.maxTotalBytes <= 0L) return PerfThroughputResult()

        val client = okHttp.newBuilder()
            /*
             * Dedicated pool so this test is not accidentally warmed by previous
             * app requests. Within this collector, repeated attempts may still
             * reuse the connection, which is okay for throughput.
             */
            .connectionPool(ConnectionPool())
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()

        var remainingBudget = safeConfig.maxTotalBytes
        var totalPayloadBytes = 0L

        val downSamples = mutableListOf<Double>()
        val upSamples = mutableListOf<Double>()

        val downloadUrl = safeConfig.downloadUrl
            ?.toHttpUrlOrNull()
            ?.takeIf { safeConfig.isAllowedUrl(it) }

        if (downloadUrl != null && safeConfig.downloadAttempts > 0) {
            repeat(safeConfig.downloadAttempts) {
                if (remainingBudget <= 0L) return@repeat

                val bytesToRead = minOf(
                    safeConfig.downloadBytesPerAttempt,
                    remainingBudget,
                )

                val sample = downloadProbe(
                    client = client,
                    url = downloadUrl,
                    maxBytesToRead = bytesToRead,
                )

                if (sample != null) {
                    downSamples += sample.mbps
                    totalPayloadBytes += sample.bytesTransferred
                    remainingBudget -= sample.bytesTransferred
                }
            }
        }

        val uploadUrl = safeConfig.uploadUrl
            ?.toHttpUrlOrNull()
            ?.takeIf { safeConfig.uploadEnabled }
            ?.takeIf { safeConfig.isAllowedUrl(it) }

        if (uploadUrl != null && safeConfig.uploadAttempts > 0) {
            repeat(safeConfig.uploadAttempts) {
                if (remainingBudget <= 0L) return@repeat

                val bytesToUpload = minOf(
                    safeConfig.uploadBytesPerAttempt,
                    remainingBudget,
                )

                val sample = uploadProbe(
                    client = client,
                    url = uploadUrl,
                    bytesToUpload = bytesToUpload,
                )

                if (sample != null) {
                    upSamples += sample.mbps
                    totalPayloadBytes += sample.bytesTransferred
                    remainingBudget -= sample.bytesTransferred
                }
            }
        }

        return PerfThroughputResult(
            downMbps = average(downSamples),
            upMbps = average(upSamples),

            downP95Mbps = percentile(downSamples, 0.95),
            upP95Mbps = percentile(upSamples, 0.95),

            downStdDevMbps = stdDev(downSamples),
            upStdDevMbps = stdDev(upSamples),

            testPayloadBytes = totalPayloadBytes.takeIf { it > 0L },
        )
    }

    private data class ThroughputSample(
        val mbps: Double,
        val bytesTransferred: Long,
        val durationMs: Long,
    )

    private fun downloadProbe(
        client: OkHttpClient,
        url: HttpUrl,
        maxBytesToRead: Long,
    ): ThroughputSample? {
        if (maxBytesToRead <= 0L) return null

        val request = Request.Builder()
            .url(url)
            .get()
            /*
             * Ask static-file servers to send only the configured byte range.
             * Some servers may ignore Range; we still stop reading locally after
             * maxBytesToRead.
             */
            .header("Range", "bytes=0-${maxBytesToRead - 1L}")
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .header("Accept-Encoding", "identity")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                /*
                 * Accept 200 because some servers ignore Range.
                 * Accept 206 because Range was honored.
                 */
                if (response.code != 200 && response.code != 206) {
                    return null
                }

                val source = response.body.source()
                val buffer = Buffer()

                var totalRead = 0L
                val startNs = System.nanoTime()

                while (totalRead < maxBytesToRead) {
                    val remaining = maxBytesToRead - totalRead
                    val readSize = minOf(READ_CHUNK_BYTES, remaining)

                    val read = source.read(buffer, readSize)
                    if (read == -1L) break

                    totalRead += read
                    buffer.clear()
                }

                val durationNs = System.nanoTime() - startNs

                if (totalRead <= 0L || durationNs <= 0L) {
                    return null
                }

                ThroughputSample(
                    mbps = mbps(
                        bytes = totalRead,
                        durationNs = durationNs,
                    ),
                    bytesTransferred = totalRead,
                    durationMs = TimeUnit.NANOSECONDS.toMillis(durationNs),
                )
            }
        } catch (_: IOException) {
            null
        } catch (e: RuntimeException) {
            if (e is CancellationException) throw e
            null
        }
    }

    private fun uploadProbe(
        client: OkHttpClient,
        url: HttpUrl,
        bytesToUpload: Long,
    ): ThroughputSample? {
        if (bytesToUpload <= 0L) return null

        val body = GeneratedBytesRequestBody(bytesToUpload)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .build()

        return try {
            val startNs = System.nanoTime()

            client.newCall(request).execute().use { response ->
                /*
                 * Read the small response body so the exchange is fully completed.
                 */
                response.body.source().readAll(blackholeSink())

                val durationNs = System.nanoTime() - startNs

                if (!response.isSuccessful || durationNs <= 0L) {
                    return null
                }

                ThroughputSample(
                    mbps = mbps(
                        bytes = bytesToUpload,
                        durationNs = durationNs,
                    ),
                    bytesTransferred = bytesToUpload,
                    durationMs = TimeUnit.NANOSECONDS.toMillis(durationNs),
                )
            }
        } catch (_: IOException) {
            null
        } catch (e: RuntimeException) {
            if (e is CancellationException) throw e
            null
        }
    }

    private class GeneratedBytesRequestBody(
        private val byteCount: Long,
    ) : RequestBody() {

        override fun contentType(): MediaType =
            "application/octet-stream".toMediaType()

        override fun contentLength(): Long = byteCount

        override fun writeTo(sink: BufferedSink) {
            val chunk = ByteArray(WRITE_CHUNK_BYTES) { 0x5A.toByte() }
            var remaining = byteCount

            while (remaining > 0L) {
                val toWrite = minOf(chunk.size.toLong(), remaining).toInt()
                sink.write(chunk, 0, toWrite)
                remaining -= toWrite
            }
        }
    }

    private fun PerfThroughputTestConfig.normalized(): PerfThroughputTestConfig {
        return copy(
            downloadBytesPerAttempt = downloadBytesPerAttempt.coerceAtLeast(0L),
            uploadBytesPerAttempt = uploadBytesPerAttempt.coerceAtLeast(0L),
            downloadAttempts = downloadAttempts.coerceAtLeast(0),
            uploadAttempts = uploadAttempts.coerceAtLeast(0),
            maxTotalBytes = maxTotalBytes.coerceAtLeast(0L),
        )
    }

    private fun PerfThroughputTestConfig.isAllowedUrl(url: HttpUrl): Boolean {
        return !(requireHttps && url.scheme != "https")
    }

    private fun mbps(
        bytes: Long,
        durationNs: Long,
    ): Double {
        val seconds = durationNs / 1_000_000_000.0
        return bytes * 8.0 / seconds / 1_000_000.0
    }

    private fun average(values: List<Double>): Double? {
        return values
            .takeIf { it.isNotEmpty() }
            ?.average()
    }

    private fun percentile(
        values: List<Double>,
        p: Double,
    ): Double? {
        if (values.isEmpty()) return null

        val sorted = values.sorted()
        val idx = ceil(p * sorted.size)
            .toInt()
            .coerceIn(1, sorted.size) - 1

        return sorted[idx]
    }

    private fun stdDev(values: List<Double>): Double? {
        if (values.size < 2) return null

        val avg = values.average()
        val variance = values.sumOf { value ->
            val diff = value - avg
            diff * diff
        } / values.size

        return sqrt(variance)
    }
}