package com.example.transportapp.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration harness smoke (Phase2.md S1): proves schema JSON v1 is exported, committed and
 * loadable. Every later sprint adds `1 -> N` migrations verified through this same harness —
 * "a migration that loses an unsynced bilty is the worst bug this app can ship" (§18.2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationSmokeTest {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `v1 schema is exported and creatable`() {
        // Robolectric resolves relative names against a data dir; the helper's driver wants the
        // absolute path — pass it explicitly to avoid the name/path mismatch.
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath
        val database = helper.createDatabase(dbName, version = 1)
        // The outbox table from the Phase 2 schema must exist at v1.
        val cursor = database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'outbox'")
        assertTrue(cursor.moveToFirst())
        cursor.close()
        database.close()
    }

    companion object {
        private const val DB_NAME = "migration-smoke-test.db"
    }
}
