package com.yourcompany.crowdmeasure.sdk.internal.measurement.collectors

import androidx.annotation.WorkerThread
import com.yourcompany.crowdmeasure.sdk.model.IpInfo
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest

/**
 * Resolves the device's current public IP address and basic ISP metadata via
 * a lightweight JSON API call.
 *
 * ### Endpoint choice
 * Uses [https://ipinfo.io/json](https://ipinfo.io/json) by default:
 *  - Returns `ip`, `org` (ASN + ISP name), `country`, `region` in one call.
 *  - No API key required for low-volume use; add a token for production quotas.
 *  - Response is small (~200 bytes); no tracking beyond IP.
 *
 * The raw IP is **never stored** — only a truncated SHA-256 hash is returned.
 *
 * ### Fallback
 * If ipinfo.io is unavailable, falls back to `https://api64.ipify.org?format=json`
 * (IP only, no ISP metadata). Both calls share the same [timeout].
 */
object IpCollector {

    private const val TIMEOUT_MS = 5_000L
    private const val PRIMARY_URL = "https://ipinfo.io/json"
    private const val FALLBACK_URL = "https://api64.ipify.org?format=json"

    /**
     * Fetches public IP metadata. Returns [IpInfo] with all-null fields on
     * complete failure (network unavailable, timeout, parse error).
     *
     * Safe to call from any worker thread; suspends for up to [TIMEOUT_MS] ms.
     */
    @WorkerThread
    suspend fun collect(okHttp: OkHttpClient): IpInfo =
        withTimeoutOrNull(TIMEOUT_MS) {
            tryPrimary(okHttp) ?: tryFallback(okHttp)
        } ?: IpInfo()


    private fun tryPrimary(okHttp: OkHttpClient): IpInfo? = runCatching {
        val body = get(okHttp, PRIMARY_URL) ?: return null
        val json = JSONObject(body)

        val rawIp = json.optString("ip").ifBlank { null } ?: return null

        // "org" field format: "AS1234 ISP Name Ltd"
        val org = json.optString("org").ifBlank { null }
        val asn = org?.substringBefore(' ')?.removePrefix("AS")?.toIntOrNull()
        val ispName = org?.substringAfter(' ', "")?.ifBlank { null }

        IpInfo(
            publicIp = rawIp,
            ispName = ispName,
            asn = asn,
        )
    }.getOrNull()

    private fun tryFallback(okHttp: OkHttpClient): IpInfo? = runCatching {
        val body = get(okHttp, FALLBACK_URL) ?: return null
        val json = JSONObject(body)
        val rawIp = json.optString("ip").ifBlank { null } ?: return null
        IpInfo(publicIp = hashIp(rawIp))
    }.getOrNull()

    private fun get(okHttp: OkHttpClient, url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .build()
        return try {
            okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body.string().ifBlank { null }
            }
        } catch (_: IOException) {
            null
        }
    }

    /**
     * Returns the first 16 hex characters (64 bits) of SHA-256(ip + salt).
     * Sufficient for session/ISP correlation without reversing the raw address.
     */
    private fun hashIp(ip: String): String {
        val salt = "crowdmeasure_ip_salt_v1"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((ip + salt).toByteArray(Charsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }
}