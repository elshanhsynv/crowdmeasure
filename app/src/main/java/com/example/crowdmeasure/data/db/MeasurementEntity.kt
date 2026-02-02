package com.example.crowdmeasure.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val measurementId: String,
    val timestampUtcMs: Long,
    val transport: String,
    val feedbackTag: String?,
    val json: String,
    val recordState: String // see domain RecordState
)