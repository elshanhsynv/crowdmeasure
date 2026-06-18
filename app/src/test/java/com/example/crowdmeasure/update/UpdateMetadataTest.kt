package com.example.crowdmeasure.update

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateMetadataTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun parseValidMetadata() {
        val metadata = json.decodeFromString<UpdateMetadata>(
            """
            {
              "versionCode":104,
              "versionName":"1.0.4",
              "apkUrl":"https://example.com/releases/app-v104.apk",
              "sha256":"E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
              "forceUpdate":true,
              "releaseNotes":"Bug fixes"
            }
            """.trimIndent()
        ).validate()

        assertEquals(104, metadata.versionCode)
        assertEquals("1.0.4", metadata.versionName)
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            metadata.sha256
        )
        assertTrue(metadata.forceUpdate)
    }

    @Test
    fun missingRequiredFieldsFailParsing() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<UpdateMetadata>(
                """{"versionCode":104,"apkUrl":"https://example.com/app.apk"}"""
            )
        }
    }

    @Test
    fun invalidHashFailsValidation() {
        val metadata = UpdateMetadata(
            versionCode = 104,
            apkUrl = "https://example.com/app.apk",
            sha256 = "not-a-hash"
        )

        assertThrows(IllegalArgumentException::class.java) {
            metadata.validate()
        }
    }

    @Test
    fun nonHttpsApkUrlFailsValidation() {
        val metadata = UpdateMetadata(
            versionCode = 104,
            apkUrl = "http://example.com/app.apk",
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        assertThrows(IllegalArgumentException::class.java) {
            metadata.validate()
        }
    }

    @Test
    fun versionComparisonUsesVersionCodeOnly() {
        val metadata = UpdateMetadata(
            versionCode = 104,
            versionName = "1.0.4",
            apkUrl = "https://example.com/app.apk",
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        )

        assertTrue(metadata.isNewerThan(103))
        assertFalse(metadata.isNewerThan(104))
        assertFalse(metadata.isNewerThan(105))
    }
}
