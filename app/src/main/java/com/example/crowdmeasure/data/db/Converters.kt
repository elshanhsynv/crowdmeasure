package com.example.crowdmeasure.data.db

import com.example.crowdmeasure.domain.model.Measurement
import kotlinx.serialization.json.Json

object Converters {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        explicitNulls = false
    }

    fun measurementToJson(m: Measurement): String = json.encodeToString(Measurement.serializer(), m)
    fun jsonToMeasurement(s: String): Measurement = json.decodeFromString(Measurement.serializer(), s)
}