package com.example.crowdmeasure.data.measurement.collectors

import androidx.annotation.WorkerThread
import com.example.crowdmeasure.domain.model.ServerInfo
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object ServerCollector {

    /**
     * Executes a lightweight pre-test ping using OkHttp to gather server details natively.
     *
     * @param okHttpClient The client to use for the request.
     * @param targetUrl The URL of the server to test (e.g., "https://baku.test.com/ping").
     * @return ServerInfo if the request succeeds, or null if the server is unreachable.
     */
    @WorkerThread
    fun collect(
        okHttpClient: OkHttpClient,
        targetUrl: String,
    ): ServerInfo? {

        var resolvedIp = "unknown"

        val ipTracker = object : EventListener() {
            override fun dnsEnd(
                call: Call,
                domainName: String,
                inetAddressList: List<InetAddress>
            ) {
                super.dnsEnd(call, domainName, inetAddressList)
                // Grab the first resolved IP OkHttp intends to use
                resolvedIp = inetAddressList.firstOrNull()?.hostAddress ?: "unknown"
            }
        }

        val request = Request.Builder()
            .url(targetUrl)
            .head()
            .header("Cache-Control", "no-cache")
            .build()

        val startNanos = System.nanoTime()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                val endNanos = System.nanoTime()
                val latencyMs = TimeUnit.NANOSECONDS.toMillis(endNanos - startNanos)

                ServerInfo(
                    serverId = extractServerId(response.headers),
                    host = request.url.host,
                    ip = resolvedIp,
                    pretestLatencyMs = latencyMs
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractServerId(headers: Headers): String {
        // Cloudflare: CF-RAY = "<ray_id>-<IATA>" e.g. "7c1abc-GYD" (Baku)
        headers["cf-ray"]?.let { v ->
            val dash = v.lastIndexOf('-')
            if (dash in 1 until v.lastIndex) return v.substring(dash + 1)
        }
        // CloudFront: X-Amz-Cf-Pop e.g. "FRA56-P1"
        headers["x-amz-cf-pop"]?.let { return it }
        // Fastly: X-Served-By
        headers["x-served-by"]?.let {
            return it.split(',', ' ').firstOrNull { t -> t.isNotBlank() } ?: "unknown"
        }
        // Akamai
        headers["x-akamai-edgescape"]?.let { return it }

        // Generic fallback
        return headers["server"] ?: "unknown"
    }
}