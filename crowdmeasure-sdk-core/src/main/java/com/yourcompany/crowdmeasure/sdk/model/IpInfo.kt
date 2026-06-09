package com.yourcompany.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable

/**
 * Public-network identity snapshot derived from an IP-geolocation lookup.
 *
 * [publicIp] — SHA-256 of the raw public IP, truncated to 16 hex chars (64 bits).
 *   Provides enough entropy for session correlation without exposing the IP itself.
 *
 * [ispName] — carrier/ISP name as reported by the lookup service (e.g. "AS1234 Vodafone").
 *
 * [asn] — Autonomous System Number; useful for ISP-level aggregation server-side.
 *
 * All fields are nullable: null means the lookup failed or timed out.
 */
@Serializable
data class IpInfo(
    val publicIp: String? = null,
    val ispName: String? = null,
    val asn: Int? = null,
)