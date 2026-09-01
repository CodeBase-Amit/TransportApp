package com.example.transportapp.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S11: 8 → 9 adds TEMPLATE_E (Phase 3 S11) — templates as data, versions as rows.
 * Proves schema validity, that a template row can be written at v9, and that the
 * (company, template_key, version) unique index refuses a duplicate version.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration8to9Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 8 to 9 creates TEMPLATE_E with unique template versions`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 8).use { v8 ->
            v8.execSQL(
                """
                INSERT INTO FREIGHT_BILL_E (
                    local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id,
                    series_id, bill_no, state, party_id, period_start, period_end, due_at,
                    freight_paise, other_charges_paise, taxable_paise, gst_paise, total_paise,
                    gst_treatment, notes, issued_at, issued_by_name, cancelled_at
                ) VALUES (
                    'fb-1', NULL, 1, NULL, 'SYNCED', NULL, 'c-1',
                    'cs-1', 'FB/IND/2627/00311', 'ISSUED', 'p-1', 1, 2, 3,
                    8642000, 0, 8642000, 432100, 9074100,
                    'INTERSTATE', NULL, 2, 'Mahesh Patidar', NULL
                )
                """.trimIndent(),
            )
        }

        val v9 = helper.runMigrationsAndValidate(dbName, version = 9, validateDroppedTables = true, TransportDatabase.MIGRATION_8_9)

        v9.execSQL(
            """
            INSERT INTO TEMPLATE_E (
                local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id,
                template_key, version, is_active, schema_version, content_json, content_hash,
                visibility, created_by_name
            ) VALUES (
                't-1', NULL, 1, NULL, 'SYNCED', NULL, 'c-1',
                'tpl-bilty-default', 1, 1, 1, '{ "schemaVersion": 1 }', 'abc123',
                'BUILT-IN', 'Engine'
            )
            """.trimIndent(),
        )
        v9.query("SELECT version, is_active FROM TEMPLATE_E WHERE template_key = 'tpl-bilty-default'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
            assertEquals(1, cursor.getInt(1))
        }

        var refused = false
        try {
            v9.execSQL(
                "INSERT INTO TEMPLATE_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id, template_key, version, is_active, schema_version, content_json, content_hash, visibility, created_by_name) " +
                    "VALUES ('t-2', NULL, 2, NULL, 'SYNCED', NULL, 'c-1', 'tpl-bilty-default', 1, 0, 1, '{}', 'def456', 'BUILT-IN', 'Engine')",
            )
        } catch (expected: Exception) {
            refused = true
        }
        assertTrue("the (company, template_key, version) unique index refuses a duplicate version", refused)
        v9.close()
    }

    companion object {
        private const val DB_NAME = "migration-8-9-test.db"
    }
}
