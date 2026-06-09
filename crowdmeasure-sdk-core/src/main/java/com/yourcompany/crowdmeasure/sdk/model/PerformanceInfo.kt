package com.yourcompany.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable

@Serializable
data class PerformanceInfo(
    val endpointId: String,

    /**
     * DNS lookup duration for the first observed DNS lookup in this run.
     *
     * Null means OkHttp did not perform a DNS lookup during the measured calls,
     * or the timing was not observable.
     */
    val dnsMs: Long? = null,

    /**
     * Connection establishment duration for the first observed new connection.
     *
     * This is intentionally named connectMs rather than tcpMs because OkHttp's
     * connection phase should not be overclaimed as pure TCP socket time.
     */
    val connectMs: Long? = null,

    /**
     * TLS handshake duration for the first observed TLS handshake.
     *
     * Null may mean the connection was reused, TLS timing was not observable,
     * or the request failed before TLS.
     */
    val tlsMs: Long? = null,

    /**
     * Average time from request headers being sent to response headers starting.
     *
     * This is application-level HTTP TTFB, not full request duration.
     */
    val ttfbAvgMs: Long? = null,

    /**
     * 95th percentile of per-probe TTFB values.
     */
    val ttfbP95Ms: Long? = null,

    /**
     * Average HTTP probe latency.
     *
     * Measured from immediately before execute() until either:
     * - the first response-body byte is available, or
     * - response headers are received for an empty-body response.
     *
     * This is not ICMP RTT.
     */
    val httpLatencyAvgMs: Long? = null,

    /**
     * 95th percentile HTTP probe latency.
     */
    val httpLatencyP95Ms: Long? = null,

    /**
     * Average absolute difference between consecutive successful HTTP latency samples.
     */
    val jitterMs: Long? = null,

    /**
     * Percentage of probes that failed at the application/HTTP client level.
     *
     * This is not raw network packet loss.
     */
    val probeFailurePct: Double? = null,

    val probesAttempted: Int = 0,
    val probesSucceeded: Int = 0,
    val probesFailed: Int = 0,

    val stallsCount: Int? = null,
    val maxStallMs: Long? = null,

    /**
     * First HTTP status observed from a completed HTTP response.
     *
     * Non-2xx statuses are still valid transport-level responses.
     */
    val httpStatus: Int? = null,

    /**
     * CDN/server edge region if exposed through known headers.
     *
     * Null when no known region-like header is available.
     */
    val serverRegion: String? = null,

    /**
     * Whether the first successful response had at least one body byte available.
     */
    val firstResponseBodyStarted: Boolean? = null,

    val protocol: ProtocolType = ProtocolType.UNKNOWN,

    /**
     * Reserved for future throughput tests.
     */
    val testPayloadBytes: Long? = null,

    // Throughput
    val downMbps: Double? = null,
    val upMbps: Double? = null,
    val downP95Mbps: Double? = null,
    val downStdDevMbps: Double? = null,
    val upP95Mbps: Double? = null,
    val upStdDevMbps: Double? = null,
)