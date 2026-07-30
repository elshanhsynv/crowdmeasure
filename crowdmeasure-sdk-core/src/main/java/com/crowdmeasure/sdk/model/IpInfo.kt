package com.crowdmeasure.sdk.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Public-network identity snapshot derived from an IP-geolocation lookup.
 *
 * [publicIp] — public IP address returned by the lookup service.
 *
 * [ispName] — carrier/ISP name as reported by the lookup service (e.g. "AS1234 Vodafone").
 *
 * [asn] — Autonomous System Number; useful for ISP-level aggregation server-side.
 *
 * All fields are nullable: If it is null then it means the lookup failed or timed out.
 */
@Serializable
data class IpInfo(
    @SerialName("publicIp")
    val publicIp: String? = null,
    val ispName: String? = null,
    val asn: Int? = null,
)
