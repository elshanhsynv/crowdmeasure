package com.example.crowdmeasure.data.measurement.collectors

import com.example.crowdmeasure.domain.model.PerformanceInfo
import com.example.crowdmeasure.domain.model.ProtocolType
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.roundToLong

object PerformanceTester {
    data class Timings(
        var dnsMs: Long? = null,
        var tcpMs: Long? = null,
        var tlsMs: Long? = null,
        var ttfbMs: Long? = null,
        var protocol: ProtocolType = ProtocolType.UNKNOWN
    )

    private data class ProbeResult(
        val rttMs: Long,
        val httpStatus: Int?,
        val serverRegion: String?
    )

    fun run(
        okHttp: OkHttpClient,
        endpointUrl: String,
        endpointId: String,
        protocolHint: ProtocolType
    ): PerformanceInfo {
        val url = URL(endpointUrl)
        require(url.protocol == "https") { "HTTPS only (even before backend upload is added)." }

        val timings = Timings(protocol = protocolHint)

        val listener = object : EventListener() {
            private var dnsStart: Long = 0
            private var connectStart: Long = 0
            private var secureStart: Long = 0
            private var reqHeadersEnd: Long = 0

            override fun dnsStart(call: okhttp3.Call, domainName: String) {
                dnsStart = now()
            }

            override fun dnsEnd(
                call: okhttp3.Call,
                domainName: String,
                inetAddressList: List<java.net.InetAddress>
            ) {
                timings.dnsMs = delta(dnsStart)
            }

            override fun connectStart(
                call: okhttp3.Call,
                inetSocketAddress: java.net.InetSocketAddress,
                proxy: java.net.Proxy
            ) {
                connectStart = now()
            }

            override fun connectEnd(
                call: okhttp3.Call,
                inetSocketAddress: java.net.InetSocketAddress,
                proxy: java.net.Proxy,
                protocol: okhttp3.Protocol?
            ) {
                timings.tcpMs = delta(connectStart)
                timings.protocol = when (protocol) {
                    okhttp3.Protocol.HTTP_2 -> ProtocolType.HTTP2
                    okhttp3.Protocol.HTTP_1_1 -> ProtocolType.HTTP1_1
                    else -> ProtocolType.UNKNOWN
                }
            }

            override fun secureConnectStart(call: okhttp3.Call) {
                secureStart = now()
            }

            override fun secureConnectEnd(call: okhttp3.Call, handshake: okhttp3.Handshake?) {
                timings.tlsMs = delta(secureStart)
            }

            override fun requestHeadersEnd(call: okhttp3.Call, request: Request) {
                reqHeadersEnd = now()
            }

            override fun responseHeadersStart(call: okhttp3.Call) {
                // Approx TTFB = response headers start - request headers end
                if (reqHeadersEnd > 0) timings.ttfbMs = (now() - reqHeadersEnd).coerceAtLeast(0)
            }
        }

        val client = okHttp.newBuilder()
            .eventListener(listener)
            .build()

        // RTT/jitter/loss approximation: multiple small GETs; failures count as loss
        val samples = mutableListOf<Long>()
        var failures = 0
        val attempts = 8

        var firstHttpStatus: Int? = null
        var firstServerRegion: String? = null

        val stallThresholdMs = 1500L
        var stalls = 0
        var maxStallMs: Long? = null

        for (i in 0 until attempts) {
            val res = singleRequestProbe(client, endpointUrl)
            if (res == null) {
                failures++
            } else {
                samples += res.rttMs

                if (firstHttpStatus == null) firstHttpStatus = res.httpStatus
                if (firstServerRegion == null) firstServerRegion = res.serverRegion

                if (res.rttMs >= stallThresholdMs) {
                    stalls++
                    maxStallMs = maxOf(maxStallMs ?: 0L, res.rttMs)
                }
            }
        }

        val rttAvg = samples.takeIf { it.isNotEmpty() }?.average()?.roundToLong()
        val rttP95 = percentile(samples, 0.95)
        val jitter = jitter(samples)
        val lossPct = ((failures.toDouble() / attempts.toDouble()) * 100.0).takeIf { true }

        return PerformanceInfo(
            endpointId = endpointId,
            dnsMs = timings.dnsMs,
            tcpMs = timings.tcpMs,
            tlsMs = timings.tlsMs,
            ttfbMs = timings.ttfbMs,
            rttAvgMs = rttAvg,
            rttP95Ms = rttP95,
            jitterMs = jitter,
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
            protocol = timings.protocol
        )
    }

    private fun singleRequestProbe(client: OkHttpClient, url: String): ProbeResult? {
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Cache-Control", "no-cache")
            .build()

        val start = now()
        return try {
            client.newCall(req).execute().use { resp ->
                resp.body.source().request(1)
                val end = now()
                val status = resp.code
                val region = extractServerRegion(resp.headers)

                ProbeResult(
                    rttMs = (end - start).coerceAtLeast(0),
                    httpStatus = status,
                    serverRegion = region
                )
            }
        } catch (_: IOException) {
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractServerRegion(headers: okhttp3.Headers): String? {
        // Best-effort. Works only if endpoint/CDN provides these headers.
        // Cloudflare: CF-RAY (e.g. "7c1...-AMS")
        headers["cf-ray"]?.let { v ->
            val dash = v.lastIndexOf('-')
            if (dash in 1 until v.length - 1) return v.substring(dash + 1)
        }

        // CloudFront: X-Amz-Cf-Pop (e.g. "FRA56-P1")
        headers["x-amz-cf-pop"]?.let { return it }

        // Fastly: X-Served-By or X-Cache (varies)
        headers["x-served-by"]?.let { return it.split(' ').firstOrNull() }

        // Akamai: X-Akamai-Edgescape (format varies)
        headers["x-akamai-edgescape"]?.let { return it }

        // Generic: Server header (not really “region”, but can be useful)
        return headers["server"]
    }

    private fun now(): Long = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
    private fun delta(startMs: Long): Long? = if (startMs <= 0) null else (now() - startMs).coerceAtLeast(0)
    private fun percentile(samples: List<Long>, p: Double): Long? {
        if (samples.isEmpty()) return null
        val sorted = samples.sorted()
        val idx = ceil(p * sorted.size).toInt().coerceIn(1, sorted.size) - 1
        return sorted[idx]
    }
    private fun jitter(samples: List<Long>): Long? {
        if (samples.size < 2) return null
        var sum = 0L
        for (i in 1 until samples.size) sum += kotlin.math.abs(samples[i] - samples[i - 1])
        return (sum.toDouble() / (samples.size - 1)).roundToLong()
    }
}
