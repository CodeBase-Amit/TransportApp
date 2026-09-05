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
 * S27 (D66): 12 → 13 adds the nine T25 letterhead columns to COMPANY_E. The profile screen
 * edits fifteen fields but the save persisted six — the rest were silently discarded.
 * Nullable: every pre-S27 company survives with null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration12to13Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 12 to 13 adds the letterhead columns and preserves rows`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 12).use { v12 ->
            v12.execSQL(
                """
                INSERT INTO COMPANY_E (
                    local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at,
                    name, legal_name, address, gstin, pan, transporter_id, gst_treatment, display_bilty_series, logo_ref
                ) VALUES (
                    'c-1', NULL, 1, NULL, 'SYNCED', NULL,
                    'Shivshakti Roadlines', 'Shivshakti Roadlines Pvt Ltd', 'Transport Nagar', NULL, NULL, NULL, 'FORWARD', NULL, NULL
                )
                """.trimIndent(),
            )
        }

        val v13 = helper.runMigrationsAndValidate(dbName, version = 13, validateDroppedTables = true, TransportDatabase.MIGRATION_12_13)

        // The pre-migration company survives with all nine letterhead fields null.
        v13.query("SELECT constitution, city, pincode, state, phone, alt_phone, email, website, footer_clause FROM COMPANY_E WHERE local_id = 'c-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            for (i in 0 until cursor.columnCount) {
                assertEquals("column ${cursor.getColumnName(i)} must be null after the migration", null, cursor.getString(i))
            }
        }
        // And each can then be set.
        v13.execSQL(
            "UPDATE COMPANY_E SET city = 'Indore', phone = '+91 98260 00000' WHERE local_id = 'c-1'",
        )
        v13.query("SELECT city, phone FROM COMPANY_E WHERE local_id = 'c-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Indore", cursor.getString(0))
            assertEquals("+91 98260 00000", cursor.getString(1))
        }
        v13.close()
    }

    companion object {
        private const val DB_NAME = "migration-12-13-test.db"
    }
}
