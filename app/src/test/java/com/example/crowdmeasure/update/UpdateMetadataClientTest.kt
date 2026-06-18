package com.example.crowdmeasure.update

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class UpdateMetadataClientTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun fetchLatestParsesSuccessfulResponse() {
        val client = UpdateMetadataClient(
            okHttpClient = okHttpReturning(
                code = 200,
                body = """
                    {
                      "versionCode":104,
                      "versionName":"1.0.4",
                      "apkUrl":"https://example.com/releases/app-v104.apk",
                      "sha256":"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                      "forceUpdate":false
                    }
                """.trimIndent()
            ),
            json = json
        )

        assertEquals(104, client.fetchLatest().versionCode)
    }

    @Test
    fun fetchLatestFailsForHttpError() {
        val client = UpdateMetadataClient(
            okHttpClient = okHttpReturning(code = 500, body = "server error"),
            json = json
        )

        assertThrows(IllegalStateException::class.java) {
            client.fetchLatest()
        }
    }

    private fun okHttpReturning(code: Int, body: String): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code in 200..299) "OK" else "Error")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
}
