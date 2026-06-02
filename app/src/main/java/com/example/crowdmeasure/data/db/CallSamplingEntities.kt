package com.example.crowdmeasure.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_sessions",
    indices = [Index("startedAtUtcMs")]
)
data class CallSessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAtUtcMs: Long,
    val endedAtUtcMs: Long?,
    val sampleIntervalSeconds: Int,
    val sampleCount: Int,
    val endReason: String?
)

@Entity(
    tableName = "call_cell_samples",
    foreignKeys = [
        ForeignKey(
            entity = CallSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("sessionId"),
        Index("sampledAtUtcMs")
    ]
)
data class CallCellSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: String,
    val sampledAtUtcMs: Long,
    val elapsedMs: Long,
    val cellJson: String,
    val rat: String?,
    val nrState: String?,
    val dbm: Int?,
    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val pci: Int?,
    val tac: Int?,
    val band: Int?
)
