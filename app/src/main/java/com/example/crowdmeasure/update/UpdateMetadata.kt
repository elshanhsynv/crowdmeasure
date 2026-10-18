package com.example.crowdmeasure.update

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class UpdateMetadata(
    val versionCode: Int,
    val versionName: String? = null,
    val apkUrl: String,
    val sha256: String,
    val forceUpdate: Boolean = false,
    val releaseNotes: String? = null
) {
    fun isNewerThan(currentVersionCode: Int): Boolean =
        versionCode > currentVersionCode

    fun validate(): UpdateMetadata {
        require(versionCode > 0) { "versionCode must be positive" }
        val uri = URI(apkUrl)
        require(uri.scheme == "https") { "apkUrl must use https" }
        require(uri.host?.isNotBlank() == true) { "apkUrl must include a host" }
        require(SHA_256_PATTERN.matches(sha256)) { "sha256 must be a 64-character hex string" }
        return copy(sha256 = sha256.lowercase())
    }

    companion object {
        private val SHA_256_PATTERN = Regex("^[a-fA-F0-9]{64}$")
    }
}

data class UpdateAvailability(
    val metadata: UpdateMetadata?
) {
    val hasUpdate: Boolean = metadata != null
}

class InstallPermissionRequiredException :
    IllegalStateException("Allow app installs from CrowdMeasure before installing this update.")
