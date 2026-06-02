package com.example.crowdmeasure.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MeasurementEntity::class,
        CallSessionEntity::class,
        CallCellSampleEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun callSamplingDao(): CallSamplingDao
}
