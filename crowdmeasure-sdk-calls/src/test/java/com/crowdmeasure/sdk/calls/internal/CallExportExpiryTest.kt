package com.crowdmeasure.sdk.calls.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CallExportExpiryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun deletesOnlyExportsOlderThanTheTtl() {
        val now = 10L * CALL_EXPORT_TTL_MS
        val old = folder.newFile("old.json").apply { setLastModified(now - CALL_EXPORT_TTL_MS - 1_000) }
        val fresh = folder.newFile("fresh.json").apply { setLastModified(now - CALL_EXPORT_TTL_MS + 1_000) }

        deleteExpiredCallExports(folder.root, now)

        assertFalse(old.exists())
        assertTrue(fresh.exists())
    }

    @Test
    fun missingDirectoryIsIgnored() {
        deleteExpiredCallExports(folder.root.resolve("missing"), System.currentTimeMillis())
    }
}
