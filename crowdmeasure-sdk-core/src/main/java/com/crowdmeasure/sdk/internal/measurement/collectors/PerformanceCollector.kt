package com.crowdmeasure.sdk.internal.measurement.collectors

import android.util.Log
import androidx.annotation.WorkerThread
import com.crowdmeasure.sdk.model.PerformanceInfo
import com.crowdmeasure.sdk.model.ProtocolType
import kotlinx.coroutines.CancellationException
import okhttp3.Call
import okhttp3.ConnectionPool
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
import kotlin.math.ceil
import kotlin.math.roundToLong

object PerformanceCollector {

    private const val STALL_THRESHOLD_MS = 1_500L

    /**
     * Runs [PROBE_ATTEMPTS] sequential lightweight HTTPS probes against [endpointUrl].
     *
     * This collector measures application-level HTTPS responsiveness, not raw ICMP RTT
     * and not raw IP packet loss.
     *
     * A dedicated OkHttp connection pool is used so the first probe is not warmed by
     * previous app requests made through the parent client. Subsequent probes may reuse
     * the connection intentionally.
     *
     * DNS may still be affected by Android/OS resolver cache.
     */
    @WorkerThread
    fun run(
        okHttp: OkHttpClient,
        endpointUrl: String,
        endpointId: String,
        attempts: Int = 8,
    ): PerformanceInfo {
        val parsedUrl = endpointUrl.toHttpUrl().also { url ->
            require(url.scheme == "https") {
                "HTTPS endpoints only. Got: ${url.scheme}"
            }
        }

        /*
         * This dedicated pool prevents EnvironmentCollector or other app requests
         * from warming this measurement run.
         *
         * The pool is still shared across this run's 8 probes, so probe #1 is the
         * connection setup probe and later probes can measure keep-alive behavior.
         */
        val measurementClient = okHttp.newBuilder()
            .connectionPool(ConnectionPool())
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .build()

        val probes = mutableListOf<ProbeResult>()
        var failures = 0

        repeat(attempts) {
            val probe = singleProbe(measurementClient, parsedUrl)
            if (probe == null) {
                failures++
            } else {
                probes += probe
            }
        }

        val latencySamples = probes.map { it.httpLatencyMs }
        val ttfbSamples = probes.mapNotNull { it.timings.ttfbMs }

        val stalls = latencySamples.count { it >= STALL_THRESHOLD_MS }
        val maxStallMs = latencySamples
            .filter { it >= STALL_THRESHOLD_MS }
            .maxOrNull()

        val firstSuccessfulProbe = probes.firstOrNull()

        val protocol = probes
            .firstOrNull { it.protocol != ProtocolType.UNKNOWN }
            ?.protocol
            ?: ProtocolType.UNKNOWN

        val probeFailurePct = (failures.toDouble() / attempts.toDouble()) * 100.0
        val ping = PingCollector.run(okHttp, parsedUrl)

        Log.d("PerformanceCollector", "Ping: $ping")

        return PerformanceInfo(
            endpointId = endpointId,

            dnsMs = probes.firstNotNullOfOrNull { it.timings.dnsMs },
            connectMs = probes.firstNotNullOfOrNull { it.timings.connectMs },
            tlsMs = probes.firstNotNullOfOrNull { it.timings.tlsMs },

            ttfbAvgMs = ttfbSamples
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToLong(),

            ttfbP95Ms = percentile(ttfbSamples, 0.95),

            httpLatencyAvgMs = latencySamples
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.roundToLong(),

            httpLatencyP95Ms = percentile(latencySamples, 0.95),

            jitterMs = jitter(latencySamples),

            pingAvgMs = ping.avgMs,
            pingMinMs = ping.minMs,
            pingMaxMs = ping.maxMs,
            pingJitterMs = ping.jitterMs,
            pingPacketLossPct = ping.packetLossPct,

            probeFailurePct = probeFailurePct,

            probesAttempted = attempts,
            probesSucceeded = probes.size,
            probesFailed = failures,

            stallsCount = stalls.takeIf { latencySamples.isNotEmpty() },
            maxStallMs = maxStallMs,

            httpStatus = firstSuccessfulProbe?.httpStatus,
            serverRegion = firstSuccessfulProbe?.serverRegion,
            firstResponseBodyStarted = firstSuccessfulProbe?.responseBodyStarted,

            protocol = protocol,

            testPayloadBytes = null,

            downMbps = null,
            upMbps = null,
            downP95Mbps = null,
            downStdDevMbps = null,
            upP95Mbps = null,
            upStdDevMbps = null,
        )
    }

    private data class ProbeResult(
        val httpLatencyMs: Long,
        val httpStatus: Int?,
        val serverRegion: String?,
        val protocol: ProtocolType,
        val responseBodyStarted: Boolean,
        val timings: CallTimings,
    )

    private data class CallTimings(
        var dnsMs: Long? = null,
        var connectMs: Long? = null,
        var tlsMs: Long? = null,
        var ttfbMs: Long? = null,
    )

    private fun singleProbe(
        baseClient: OkHttpClient,
        url: HttpUrl,
    ): ProbeResult? {
        val timings = CallTimings()

        /*
         * A fresh listener is attached for this one call, so timings cannot be
         * overwritten by another probe.
         */
        val client = baseClient.newBuilder()
            .eventListener(TimingEventListener(timings))
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Cache-Control", "no-store")
            .header("Pragma", "no-cache")
            .header("Accept-Encoding", "identity")
            .build()

        val startNs = System.nanoTime()

        return try {
            client.newCall(request).execute().use { response ->
                val bodyStarted = response.body.source().request(1)

                val httpLatencyMs = elapsedMs(startNs) ?: 0L

                val protocol = when (response.protocol) {
                    Protocol.HTTP_2 -> ProtocolType.HTTP2
                    Protocol.HTTP_1_1 -> ProtocolType.HTTP1_1
                    else -> ProtocolType.UNKNOWN
                }

                ProbeResult(
                    httpLatencyMs = httpLatencyMs,
                    httpStatus = response.code,
                    serverRegion = extractServerRegion(response.headers),
                    protocol = protocol,
                    responseBodyStarted = bodyStarted,
                    timings = timings,
                )
            }
        } catch (_: IOException) {
            null
        } catch (e: RuntimeException) {
            if (e is CancellationException) throw e
            null
        }
    }

    // -------------------------------------------------------------------------
    // EventListener
    //
    // One TimingEventListener instance is used per HTTP call.
    //
    // dnsMs:
    //   DNS lookup duration, if DNS was performed.
    //
    // connectMs:
    //   OkHttp connection establishment duration. This should not be overclaimed
    //   as pure TCP time.
    //
    // tlsMs:
    //   TLS handshake duration, if a TLS handshake was performed.
    //
    // ttfbMs:
    //   Time from request headers sent to response headers starting.
    // -------------------------------------------------------------------------

    private class TimingEventListener(
        private val timings: CallTimings,
    ) : EventListener() {

        private var dnsStartNs = 0L
        private var connectStartNs = 0L
        private var tlsStartNs = 0L
        private var requestHeadersEndNs = 0L

        override fun dnsStart(call: Call, domainName: String) {
            dnsStartNs = System.nanoTime()
        }

        override fun dnsEnd(
            call: Call,
            domainName: String,
            inetAddressList: List<InetAddress>,
        ) {
            timings.dnsMs = elapsedMs(dnsStartNs)
        }

        override fun connectStart(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
        ) {
            connectStartNs = System.nanoTime()
        }

        override fun connectEnd(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?,
        ) {
            timings.connectMs = elapsedMs(connectStartNs)
        }

        override fun secureConnectStart(call: Call) {
            tlsStartNs = System.nanoTime()
        }

        override fun secureConnectEnd(call: Call, handshake: Handshake?) {
            timings.tlsMs = elapsedMs(tlsStartNs)
        }

        override fun requestHeadersEnd(call: Call, request: Request) {
            requestHeadersEndNs = System.nanoTime()
        }

        override fun responseHeadersStart(call: Call) {
            if (requestHeadersEndNs > 0L) {
                timings.ttfbMs = elapsedMs(requestHeadersEndNs)
            }
        }
    }

    private fun extractServerRegion(headers: Headers): String? {
        // Cloudflare: CF-RAY = "<ray_id>-<IATA>", e.g. "7c1abc-AMS"
        headers["cf-ray"]?.let { value ->
            val dash = value.lastIndexOf('-')
            if (dash in 1 until value.lastIndex) {
                return value.substring(dash + 1).trim().takeIf { it.isNotEmpty() }
            }
        }

        // CloudFront: X-Amz-Cf-Pop, e.g. "FRA56-P1"
        headers["x-amz-cf-pop"]?.let { value ->
            return value.trim().takeIf { it.isNotEmpty() }
        }

        // Fastly: X-Served-By can be a list; take the first non-empty token.
        headers["x-served-by"]?.let { value ->
            return value
                .split(',', ' ')
                .firstOrNull { it.isNotBlank() }
                ?.trim()
        }

        // Akamai: customer-configured and format varies.
        headers["x-akamai-edgescape"]?.let { value ->
            return value.trim().takeIf { it.isNotEmpty() }
        }
        return headers["server"]
    }

    private fun percentile(samples: List<Long>, p: Double): Long? {
        if (samples.isEmpty()) return null

        val sorted = samples.sorted()
        val idx = ceil(p * sorted.size)
            .toInt()
            .coerceIn(1, sorted.size) - 1

        return sorted[idx]
    }
}
