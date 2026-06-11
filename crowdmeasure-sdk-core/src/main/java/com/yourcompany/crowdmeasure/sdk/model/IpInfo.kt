package com.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Public-network identity snapshot derived from an IP-geolocation lookup.
 *
 * [publicIpHash] — a per-install salted SHA-256 hash of the raw public IP.
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
    @SerialName("publicIp")
    val publicIpHash: String? = null,
    val ispName: String? = null,
    val asn: Int? = null,
)
