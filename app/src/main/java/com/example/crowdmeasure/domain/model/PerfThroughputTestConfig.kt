package com.example.crowdmeasure.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PerfThroughputTestConfig(
    /**
     * Enables/disables throughput collection.
     *
     * Keep false by default unless user/settings explicitly enable it.
     */
    val enabled: Boolean = false,

    /**
     * Public test-file URL or your own future download endpoint.
     *
     * Example public-file style:
     * https://example.com/test-10mb.bin
     *
     * Future own-server style:
     * https://your-server.com/download?bytes=2097152
     */
    val downloadUrl: String? = null,

    /**
     * Future upload endpoint.
     *
     * Example:
     * https://your-server.com/upload
     *
     * Leave null until you own/control the server.
     */
    val uploadUrl: String? = null,

    /**
     * Number of bytes to read per download attempt.
     *
     * Keep this small for anonymous/background collection.
     */
    val downloadBytesPerAttempt: Long = 2L * 1024L * 1024L, // 2 MiB

    /**
     * Number of bytes to upload per attempt.
     *
     * Used only when uploadUrl is non-null and upload is enabled.
     */
    val uploadBytesPerAttempt: Long = 1L * 1024L * 1024L, // 1 MiB

    /**
     * Number of download attempts.
     */
    val downloadAttempts: Int = 2,

    /**
     * Number of upload attempts.
     */
    val uploadAttempts: Int = 2,

    /**
     * Upload is more sensitive because it sends real user data volume.
     * Keep this false unless explicitly enabled.
     */
    val uploadEnabled: Boolean = false,

    /**
     * Hard safety cap for total bytes consumed by this collector.
     */
    val maxTotalBytes: Long = 10L * 1024L * 1024L, // 10 MiB

    /**
     * Require HTTPS URLs.
     */
    val requireHttps: Boolean = true,
)