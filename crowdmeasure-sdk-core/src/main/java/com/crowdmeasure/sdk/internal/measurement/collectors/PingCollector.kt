package com.crowdmeasure.sdk.internal.measurement.collectors

import androidx.annotation.WorkerThread
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.InetSocketAddress
import kotlin.math.roundToLong

internal object PingCollector {

    private const val ATTEMPTS = 5
    private const val CONNECT_TIMEOUT_MS = 5_000

    @WorkerThread
    fun run(
        okHttp: OkHttpClient,
        url: HttpUrl,
    ): PingResult {
        checkActive()

        val addresses = try {
            okHttp.dns.lookup(url.host)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return PingResult.failed(ATTEMPTS)
        }

        val address = addresses.firstOrNull()
            ?: return PingResult.failed(ATTEMPTS)
        val socketAddress = InetSocketAddress(address, url.port)
        val samples = ArrayList<Long>(ATTEMPTS)
        var failures = 0

        repeat(ATTEMPTS) {
            checkActive()

            val sample = connectOnce(okHttp, socketAddress)
            if (sample == null) {
                failures++
            } else {
                samples += sample
            }
        }

        return PingResult.from(samples, failures, ATTEMPTS)
    }

    private fun connectOnce(
        okHttp: OkHttpClient,
        socketAddress: InetSocketAddress,
    ): Long? {
        val startNs = System.nanoTime()

        return try {
            okHttp.socketFactory.createSocket().use { socket ->
                socket.connect(socketAddress, CONNECT_TIMEOUT_MS)
                elapsedMs(startNs) ?: 0L
            }
        } catch (_: IOException) {
            checkActive()
            null
        } catch (e: RuntimeException) {
            if (e is CancellationException) throw e
            checkActive()
            null
        }
    }

    private fun checkActive() {
        if (Thread.currentThread().isInterrupted) {
            throw CancellationException("Ping measurement cancelled")
        }
    }
}

internal data class PingResult(
    val avgMs: Long?,
    val minMs: Long?,
    val maxMs: Long?,
    val jitterMs: Long?,
    val packetLossPct: Double,
) {
    companion object {
        fun failed(attempts: Int): PingResult = from(emptyList(), attempts, attempts)

        fun from(
            samples: List<Long>,
            failures: Int,
            attempts: Int,
        ): PingResult {
            require(attempts > 0) { "attempts must be positive" }
            require(failures in 0..attempts) { "failures must be between 0 and attempts" }

            return PingResult(
                avgMs = samples
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.roundToLong(),
                minMs = samples.minOrNull(),
                maxMs = samples.maxOrNull(),
                jitterMs = jitter(samples),
                packetLossPct = (failures.toDouble() / attempts.toDouble()) * 100.0,
            )
        }
    }
}
