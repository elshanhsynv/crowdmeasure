package com.example.crowdmeasure.docs

import com.crowdmeasure.sdk.model.Measurement
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MockMeasurementJsonTest {

    private val mockFile = File("src/main/java/com/example/crowdmeasure/docs/demo_measurement_complete.json")
    private val measurementList = ListSerializer(Measurement.serializer())

    @Test
    fun mockDecodesStrictlyIntoTheModel() {
        val measurements = Json.decodeFromString(measurementList, mockFile.readText())

        assertEquals(1, measurements.size)
    }

    @Test
    fun mockHasEveryFieldTheModelWrites() {
        val decoded = Json.decodeFromString(measurementList, mockFile.readText())
        val fullJson = Json { encodeDefaults = true; explicitNulls = true }
        val modelKeys = keyPaths(fullJson.encodeToJsonElement(measurementList, decoded))
        val mockKeys = keyPaths(Json.parseToJsonElement(mockFile.readText()))

        assertTrue("Missing from mock: ${modelKeys - mockKeys}", (modelKeys - mockKeys).isEmpty())
        assertTrue("Not in model: ${mockKeys - modelKeys}", (mockKeys - modelKeys).isEmpty())
    }

    /** Every object key path in [element]; array elements share the path segment `[]`. */
    private fun keyPaths(element: JsonElement, prefix: String = ""): Set<String> = when (element) {
        is JsonObject -> element.flatMap { (key, value) ->
            val path = if (prefix.isEmpty()) key else "$prefix.$key"
            setOf(path) + keyPaths(value, path)
        }.toSet()
        is JsonArray -> element.flatMap { keyPaths(it, "$prefix[]") }.toSet()
        else -> emptySet()
    }
}
