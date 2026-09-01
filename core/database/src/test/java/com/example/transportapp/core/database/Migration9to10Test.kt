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
 * S14: 9 → 10 adds COMPANY_SETTING_E (Phase 3 S14) — dated company calculation settings.
 * Proves schema validity, that a setting row can be written at v10, and that the governing
 * read (newest effective_from ≤ now) picks the right row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration9to10Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 9 to 10 creates COMPANY_SETTING_E with dated rows`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 9).use { v9 ->
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
        }

        val v10 = helper.runMigrationsAndValidate(dbName, version = 10, validateDroppedTables = true, TransportDatabase.MIGRATION_9_10)

        v10.execSQL(
            """
            INSERT INTO COMPANY_SETTING_E (
                local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id,
                effective_from, gst_rate_bp, weight_step_g, volumetric_divisor_g, gst_treatment, rounding, created_by_name
            ) VALUES (
                'cs-1', NULL, 1, NULL, 'SYNCED', NULL, 'c-1',
                1000, 500, 1000, 6000, 'FORWARD', 'NEAREST_RUPEE', 'Engine'
            )
            """.trimIndent(),
        )
        v10.execSQL(
            """
            INSERT INTO COMPANY_SETTING_E (
                local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id,
                effective_from, gst_rate_bp, weight_step_g, volumetric_divisor_g, gst_treatment, rounding, created_by_name
            ) VALUES (
                'cs-2', NULL, 2, NULL, 'SYNCED', NULL, 'c-1',
                2000, 1200, 1000, 6000, 'FORWARD', 'NEAREST_RUPEE', 'Mahesh Patidar'
            )
            """.trimIndent(),
        )
        v10.query("SELECT gst_rate_bp FROM COMPANY_SETTING_E WHERE company_id = 'c-1' AND effective_from <= 1500 ORDER BY effective_from DESC LIMIT 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the row governing a mid-window booking is the older one", 500, cursor.getInt(0))
        }
        v10.query("SELECT gst_rate_bp FROM COMPANY_SETTING_E WHERE company_id = 'c-1' AND effective_from <= 9000 ORDER BY effective_from DESC LIMIT 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the newest governing row wins after its effective date", 1200, cursor.getInt(0))
        }
        v10.close()
    }

    companion object {
        private const val DB_NAME = "migration-9-10-test.db"
    }
}
