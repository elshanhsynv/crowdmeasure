package com.example.crowdmeasure.data.measurement.collectors

import androidx.annotation.WorkerThread
import com.example.crowdmeasure.domain.model.PerformanceInfo
import com.example.crowdmeasure.domain.model.ProtocolType
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong

object PerformanceTester {

    private const val PROBE_ATTEMPTS = 8
    private const val STALL_THRESHOLD_MS = 1_500L

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Runs [PROBE_ATTEMPTS] sequential HEAD/GET probes against [endpointUrl] and
     * returns aggregated performance metrics.
     *
     * DNS, TCP, and TLS timings reflect the *first* (cold) connection only;
     * subsequent probes reuse the keep-alive connection intentionally.
     *
     * Must be called from a worker thread — blocks for the duration of all probes.
     */
    @WorkerThread
    fun run(
        okHttp: OkHttpClient,
        endpointUrl: String,
        endpointId: String,
    ): PerformanceInfo {
        val parsedUrl = endpointUrl.toHttpUrl().also { url ->
            require(url.scheme == "https") { "HTTPS endpoints only. Got: ${url.scheme}" }
        }

        val timingListener = TimingEventListener()

        // newBuilder() shares the parent's connection pool and thread pool;
        // only the EventListener is overridden.
        val client = okHttp.newBuilder()
            .eventListener(timingListener)
            .build()

        val samples = mutableListOf<Long>()
        var failures = 0
        var firstHttpStatus: Int? = null
        var firstServerRegion: String? = null
        var stalls = 0
        var maxStallMs: Long? = null
        var negotiatedProtocol = ProtocolType.UNKNOWN

        for (i in 0 until PROBE_ATTEMPTS) {
            val probe = singleProbe(client, parsedUrl)
            if (probe == null) {
                failures++
            } else {
                samples += probe.rttMs

                if (firstHttpStatus == null) firstHttpStatus = probe.httpStatus
                if (firstServerRegion == null) firstServerRegion = probe.serverRegion
                if (negotiatedProtocol == ProtocolType.UNKNOWN) {
                    negotiatedProtocol = probe.protocol
                }

                if (probe.rttMs >= STALL_THRESHOLD_MS) {
                    stalls++
                    maxStallMs = maxOf(maxStallMs ?: 0L, probe.rttMs)
                }
            }
        }

        // Loss is meaningful even when all probes failed (100 %).
        val lossPct = (failures.toDouble() / PROBE_ATTEMPTS.toDouble()) * 100.0

        return PerformanceInfo(
            endpointId = endpointId,
            dnsMs = timingListener.dnsMs,
            tcpMs = timingListener.tcpMs,
            tlsMs = timingListener.tlsMs,
            ttfbMs = timingListener.ttfbMs,
            rttAvgMs = samples.takeIf { it.isNotEmpty() }?.average()?.roundToLong(),
            rttP95Ms = percentile(samples, 0.95),
            jitterMs = jitter(samples),
            packetLossPct = lossPct,
            downMbps = null,
            upMbps = null,
            downP95Mbps = null,
            downStdDevMbps = null,
            upP95Mbps = null,
            upStdDevMbps = null,
            stallsCount = stalls.takeIf { samples.isNotEmpty() },
            maxStallMs = maxStallMs,
            httpStatus = firstHttpStatus,
            serverRegion = firstServerRegion,
            testPayloadBytes = null,
            protocol = negotiatedProtocol,
        )
    }

    // -------------------------------------------------------------------------
    // Probe
    // -------------------------------------------------------------------------

    private data class ProbeResult(
        val rttMs: Long,
        val httpStatus: Int?,
        val serverRegion: String?,
        val protocol: ProtocolType,
    )

    private fun singleProbe(client: OkHttpClient, url: HttpUrl): ProbeResult? {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Cache-Control", "no-cache")
            .build()

        val start = now()
        return try {
            client.newCall(request).execute().use { response ->
                // Read at least one byte to ensure the body has started arriving
                // before we record the end time (prevents measuring header-only latency).
                response.body.source().request(1)
                val rttMs = (now() - start).coerceAtLeast(0)

                // Read protocol from Response — more accurate than connectEnd,
                // which may not reflect ALPN negotiation on the first call.
                val protocol = when (response.protocol) {
                    Protocol.HTTP_2 -> ProtocolType.HTTP2
                    Protocol.HTTP_1_1 -> ProtocolType.HTTP1_1
                    else -> ProtocolType.UNKNOWN
                }

                ProbeResult(
                    rttMs = rttMs,
                    httpStatus = response.code,
                    serverRegion = extractServerRegion(response.headers),
                    protocol = protocol,
                )
            }
        } catch (_: IOException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    // -------------------------------------------------------------------------
    // EventListener — all callbacks arrive on OkHttp's internal thread.
    // Fields are written exclusively there and read only after execute() returns
    // (OkHttp guarantees a happens-before edge at call completion).
    // -------------------------------------------------------------------------

    private class TimingEventListener : EventListener() {

        var dnsMs: Long? = null; private set
        var tcpMs: Long? = null; private set
        var tlsMs: Long? = null; private set
        var ttfbMs: Long? = null; private set

        private var dnsStart = 0L
        private var connectStart = 0L
        private var tlsStart = 0L
        private var reqHeadersEnd = 0L

        override fun dnsStart(call: Call, domainName: String) {
            dnsStart = now()
        }

        override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
            dnsMs = delta(dnsStart)
        }

        override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
            connectStart = now()
        }

        override fun connectEnd(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?,
        ) {
            tcpMs = delta(connectStart)
        }

        override fun secureConnectStart(call: Call) {
            tlsStart = now()
        }

        override fun secureConnectEnd(call: Call, handshake: Handshake?) {
            tlsMs = delta(tlsStart)
        }

        override fun requestHeadersEnd(call: Call, request: Request) {
            reqHeadersEnd = now()
        }

        override fun responseHeadersStart(call: Call) {
            if (reqHeadersEnd > 0L) ttfbMs = (now() - reqHeadersEnd).coerceAtLeast(0L)
        }
    }

    private fun extractServerRegion(headers: Headers): String? {
        // Cloudflare: CF-RAY = "<ray_id>-<IATA>" e.g. "7c1abc-AMS"
        headers["cf-ray"]?.let { v ->
            val dash = v.lastIndexOf('-')
            if (dash in 1 until v.lastIndex) return v.substring(dash + 1)
        }
        // CloudFront: X-Amz-Cf-Pop e.g. "FRA56-P1"
        headers["x-amz-cf-pop"]?.let { return it }
        // Fastly: X-Served-By (may be a list; take first token)
        headers["x-served-by"]?.let { return it.split(',', ' ').firstOrNull { t -> t.isNotBlank() } }
        // Akamai: X-Akamai-Edgescape (format varies per customer config)
        headers["x-akamai-edgescape"]?.let { return it }
        // Generic fallback
        return headers["server"]
    }

    private fun now(): Long = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())

    private fun delta(startMs: Long): Long? =
        if (startMs <= 0L) null else (now() - startMs).coerceAtLeast(0L)

    private fun percentile(samples: List<Long>, p: Double): Long? {
        if (samples.isEmpty()) return null
        val sorted = samples.sorted()
        val idx = ceil(p * sorted.size).toInt().coerceIn(1, sorted.size) - 1
        return sorted[idx]
    }

    private fun jitter(samples: List<Long>): Long? {
        if (samples.size < 2) return null
        var sum = 0L
        for (i in 1 until samples.size) sum += abs(samples[i] - samples[i - 1])
        return (sum.toDouble() / (samples.size - 1)).roundToLong()
    }
}