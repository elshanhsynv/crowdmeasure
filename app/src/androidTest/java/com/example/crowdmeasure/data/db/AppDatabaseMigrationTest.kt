package com.example.crowdmeasure.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate5To6_rebuildsCallSamplesAndKeepsData() {
        helper.createDatabase(DB_NAME, 5).use { db ->
            db.execSQL(
                "INSERT INTO call_sessions (sessionId, startedAtUtcMs, endedAtUtcMs, callType, callSource, " +
                    "sampleIntervalSeconds, sampleCount, endReason, uploadState, carriersJson) " +
                    "VALUES ('s1', 1000, 2000, 'INCOMING', 'CELLULAR', 5, 1, 'ended', 'PENDING', NULL)"
            )
            db.execSQL(
                "INSERT INTO call_cell_samples (id, sessionId, sampledAtUtcMs, elapsedMs, cellJson, rat, dbm) " +
                    "VALUES (7, 's1', 1500, 500, '{\"rat\":\"LTE\"}', 'LTE', -90)"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 6, true, *Migrations.ALL)

        db.query("SELECT id, sessionId, sampledAtUtcMs, elapsedMs, cellJson FROM call_cell_samples").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals(7L, c.getLong(0))
            assertEquals("s1", c.getString(1))
            assertEquals(1500L, c.getLong(2))
            assertEquals(500L, c.getLong(3))
            assertEquals("{\"rat\":\"LTE\"}", c.getString(4))
        }

        // AUTOINCREMENT sequence survives the rebuild, so new ids never reuse old ones.
        db.execSQL(
            "INSERT INTO call_cell_samples (sessionId, sampledAtUtcMs, elapsedMs, cellJson) " +
                "VALUES ('s1', 1600, 600, '{}')"
        )
        db.query("SELECT MAX(id) FROM call_cell_samples").use { c ->
            c.moveToFirst()
            assertTrue(c.getLong(0) > 7L)
        }

        // The cascade foreign key to call_sessions is still in place.
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("DELETE FROM call_sessions WHERE sessionId = 's1'")
        db.query("SELECT COUNT(*) FROM call_cell_samples").use { c ->
            c.moveToFirst()
            assertEquals(0, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate1To6_fullChainKeepsMeasurements() {
        helper.createDatabase(DB_NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO measurements (measurementId, timestampUtcMs, transport, json, recordState) " +
                    "VALUES ('m1', 1000, 'WIFI', '{}', 'PENDING')"
            )
        }

        val db = helper.runMigrationsAndValidate(DB_NAME, 6, true, *Migrations.ALL)

        db.query("SELECT measurementId FROM measurements").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("m1", c.getString(0))
        }
        db.close()
    }

    private companion object {
        const val DB_NAME = "app-migration-test.db"
    }
}
