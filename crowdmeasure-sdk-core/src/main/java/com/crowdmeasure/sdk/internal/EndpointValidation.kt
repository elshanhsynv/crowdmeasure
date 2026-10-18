package com.crowdmeasure.sdk.internal

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * True for an `https` URL with a host and no embedded credentials. Uses OkHttp's parser so the
 * check accepts exactly the URLs the probes can request.
 */
internal fun isValidHttpsEndpoint(url: String): Boolean {
    val parsed = url.trim().toHttpUrlOrNull() ?: return false
    return parsed.isHttps && parsed.host.isNotBlank() &&
        parsed.username.isEmpty() && parsed.password.isEmpty()
}
