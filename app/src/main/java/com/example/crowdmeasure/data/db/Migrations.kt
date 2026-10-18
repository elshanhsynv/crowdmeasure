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
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS call_sessions (
                        sessionId TEXT NOT NULL PRIMARY KEY,
                        startedAtUtcMs INTEGER NOT NULL,
                        endedAtUtcMs INTEGER,
                        callType TEXT NOT NULL,
                        sampleIntervalSeconds INTEGER NOT NULL,
                        sampleCount INTEGER NOT NULL,
                        endReason TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS call_cell_samples (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        sampledAtUtcMs INTEGER NOT NULL,
                        elapsedMs INTEGER NOT NULL,
                        cellJson TEXT NOT NULL,
                        rat TEXT,
                        nrState TEXT,
                        dbm INTEGER,
                        rsrpDbm INTEGER,
                        rsrqDb INTEGER,
                        sinrDb INTEGER,
                        pci INTEGER,
                        tac INTEGER,
                        band INTEGER,
                        FOREIGN KEY(sessionId) REFERENCES call_sessions(sessionId)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_call_sessions_startedAtUtcMs ON call_sessions(startedAtUtcMs)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_call_cell_samples_sessionId ON call_cell_samples(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_call_cell_samples_sampledAtUtcMs ON call_cell_samples(sampledAtUtcMs)")
            }
        },
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE call_sessions ADD COLUMN callSource TEXT NOT NULL DEFAULT 'CELLULAR'"
                )
            }
        },
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE call_sessions ADD COLUMN uploadState TEXT NOT NULL DEFAULT 'PENDING'"
                )
            }
        },
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_sessions ADD COLUMN carriersJson TEXT")
            }
        },
        // v6 dropped the denormalized signal columns from call_cell_samples (all data lives in cellJson).
        // SQLite on minSdk 29 has no DROP COLUMN, so the table is rebuilt.
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE call_cell_samples_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId TEXT NOT NULL,
                        sampledAtUtcMs INTEGER NOT NULL,
                        elapsedMs INTEGER NOT NULL,
                        cellJson TEXT NOT NULL,
                        FOREIGN KEY(sessionId) REFERENCES call_sessions(sessionId)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO call_cell_samples_new (id, sessionId, sampledAtUtcMs, elapsedMs, cellJson)
                    SELECT id, sessionId, sampledAtUtcMs, elapsedMs, cellJson FROM call_cell_samples
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE call_cell_samples")
                db.execSQL("ALTER TABLE call_cell_samples_new RENAME TO call_cell_samples")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_call_cell_samples_sessionId ON call_cell_samples(sessionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_call_cell_samples_sampledAtUtcMs ON call_cell_samples(sampledAtUtcMs)")
            }
        }
    )
}
