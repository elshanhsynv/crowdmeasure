package com.crowdmeasure.sdk.calls.internal

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallsDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CallsDatabase::class.java,
    )

    @Test
    fun migrate1To3_keepsSessionsAndSamples() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO call_sessions (sessionId, startedAtUtcMs, endedAtUtcMs, callType, callSource, " +
                    "sampleIntervalSeconds, sampleCount, endReason, uploadState) " +
                    "VALUES ('s1', 1000, 2000, 'OUTGOING', 'CELLULAR', 5, 1, 'ended', 'UPLOADED')"
            )
            db.execSQL(
                "INSERT INTO call_cell_samples (id, sessionId, sampledAtUtcMs, elapsedMs, cellJson, rat, pci) " +
                    "VALUES (3, 's1', 1500, 500, '{}', 'NR', 42)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true, *DefaultCallStore.MIGRATIONS)

        db.query("SELECT uploadState, carriersJson FROM call_sessions WHERE sessionId = 's1'").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("UPLOADED", c.getString(0))
            assertTrue(c.isNull(1))
        }
        db.query("SELECT id, sessionId, sampledAtUtcMs, elapsedMs, cellJson FROM call_cell_samples").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(3L, c.getLong(0))
            assertEquals("s1", c.getString(1))
            assertEquals(1500L, c.getLong(2))
            assertEquals(500L, c.getLong(3))
            assertEquals("{}", c.getString(4))
        }
        db.close()
    }

    @Test
    fun migrate2To3_keepsCascadeForeignKey() {
        helper.createDatabase(DB_NAME, 2).use { db ->
            db.execSQL(
                "INSERT INTO call_sessions (sessionId, startedAtUtcMs, endedAtUtcMs, callType, callSource, " +
                    "sampleIntervalSeconds, sampleCount, endReason, uploadState, carriersJson) " +
                    "VALUES ('s1', 1000, NULL, 'INCOMING', 'CELLULAR', 5, 1, NULL, 'PENDING', '[]')"
            )
            db.execSQL(
                "INSERT INTO call_cell_samples (sessionId, sampledAtUtcMs, elapsedMs, cellJson, dbm) " +
                    "VALUES ('s1', 1500, 500, '{}', -100)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 3, true, *DefaultCallStore.MIGRATIONS)

        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM call_sessions WHERE sessionId = 's1'")
        db.query("SELECT COUNT(*) FROM call_cell_samples").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    private companion object {
        const val DB_NAME = "calls-migration-test.db"
    }
}
