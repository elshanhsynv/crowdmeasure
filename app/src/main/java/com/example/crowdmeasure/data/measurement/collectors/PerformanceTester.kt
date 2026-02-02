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

            override fun dnsEnd(call: okhttp3.Call, domainName: String, inetAddressList: List<java.net.InetAddress>) {
                timings.dnsMs = delta(dnsStart)
            }

            override fun connectStart(call: okhttp3.Call, inetSocketAddress: java.net.InetSocketAddress, proxy: java.net.Proxy) {
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

        for (i in 0 until attempts) {
            val rtt = singleRequestRtt(client, endpointUrl)
            if (rtt == null) failures++ else samples += rtt
        }

        val rttAvg = samples.takeIf { it.isNotEmpty() }?.average()?.roundToLong()
        val rttP95 = percentile(samples, 0.95)
        val jitter = jitter(samples)
        val lossPct = ((failures.toDouble() / attempts.toDouble()) * 100.0).takeIf { attempts > 0 }

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
            testPayloadBytes = null,
            protocol = timings.protocol
        )
    }

    private fun singleRequestRtt(client: OkHttpClient, url: String): Long? {
        val req = Request.Builder()
            .url(url)
            .get()
            .header("Cache-Control", "no-cache")
            .build()

        val start = now()
        return try {
            client.newCall(req).execute().use { resp ->
                // consume minimal; don't download body
                resp.body?.source()?.request(1)
                val end = now()
                (end - start).coerceAtLeast(0)
            }
        } catch (_: IOException) {
            null
        } catch (_: Throwable) {
            null
        }
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