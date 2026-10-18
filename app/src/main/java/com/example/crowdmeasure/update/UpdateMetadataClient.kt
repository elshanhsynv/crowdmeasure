package com.example.crowdmeasure.update

import com.example.crowdmeasure.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateMetadataClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    fun fetchLatest(): UpdateMetadata {
        val url = BuildConfig.UPDATE_METADATA_URL
        require(url.isNotBlank()) { "Update metadata URL is not configured" }

        val request = Request.Builder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Update metadata request failed: HTTP ${response.code}" }
            val source = response.body.source()
            // request() returns true once more than the limit is buffered, i.e. the body is too large.
            check(!source.request(MAX_METADATA_BYTES + 1)) { "Update metadata is too large" }
            val body = source.buffer.readUtf8()
            return json.decodeFromString<UpdateMetadata>(body).validate()
        }
    }

    private companion object {
        const val MAX_METADATA_BYTES = 64L * 1024
    }
}
