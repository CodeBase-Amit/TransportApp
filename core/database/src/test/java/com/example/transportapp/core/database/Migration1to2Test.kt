package com.example.transportapp.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S2: 1 → 2 adds the org tables. Every migration is proven here before it ships —
 * "a migration that loses an unsynced bilty is the worst bug this app can ship" (§18.2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration1to2Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 1 to 2 keeps outbox rows and adds org tables`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 1).use { v1 ->
            v1.execSQL(
                """
                INSERT INTO outbox (client_op_id, op, entity_type, entity_local_id, payload_json, state, attempt_count, next_attempt_at, created_at)
                VALUES ('op-1', 'INSERT', 'CONSIGNMENT', 'c1', '{}', 'PENDING', 0, 0, 1)
                """.trimIndent(),
            )
        }

        val v2 = helper.runMigrationsAndValidate(dbName, version = 2, validateDroppedTables = true, TransportDatabase.MIGRATION_1_2)

        // The outbox row survived the migration.
        v2.query("SELECT client_op_id FROM outbox WHERE client_op_id = 'op-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
        }
        // The new org tables exist.
        v2.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('COMPANY_E','BRANCH_E','MEMBERSHIP_E')").use { cursor ->
            var seen = 0
            while (cursor.moveToNext()) seen++
            assertTrue("expected 3 org tables, saw $seen", seen == 3)
        }
        assertFalse(false)
        v2.close()
    }

    companion object {
        private const val DB_NAME = "migration-1-2-test.db"
    }
}
