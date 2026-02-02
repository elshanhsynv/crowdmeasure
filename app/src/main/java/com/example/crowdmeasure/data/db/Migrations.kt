package com.example.crowdmeasure.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration strategy note:
 * - Keep Measurement stored as one JSON column + a few indexed columns (timestamp, transport, tag, state).
 * - This makes schema evolution resilient: adding new fields doesn't require DB migration.
 * - Only migrate when you need new indexed/queried columns.
 */
object Migrations {
    val ALL: Array<Migration> = arrayOf(
        object : Migration(1, 1) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // no-op
            }
        }
    )
}