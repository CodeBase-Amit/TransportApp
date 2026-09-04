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
 * S22 (D60): 11 → 12 adds COMPANY_E.logo_ref — the relative file ref of the company's
 * logo, printed in letterheads. Nullable: every pre-S22 company survives with null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration11to12Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 11 to 12 adds logo_ref and preserves rows`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 11).use { v11 ->
            v11.execSQL(
                """
                INSERT INTO COMPANY_E (
                    local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at,
                    name, legal_name, address, gstin, pan, transporter_id, gst_treatment, display_bilty_series
                ) VALUES (
                    'c-1', NULL, 1, NULL, 'SYNCED', NULL,
                    'Shivshakti Roadlines', 'Shivshakti Roadlines Pvt Ltd', 'Transport Nagar', NULL, NULL, NULL, 'FORWARD', NULL
                )
                """.trimIndent(),
            )
        }

        val v12 = helper.runMigrationsAndValidate(dbName, version = 12, validateDroppedTables = true, TransportDatabase.MIGRATION_11_12)

        // The pre-migration company survives with a null logo; a logo can then be set.
        v12.query("SELECT logo_ref FROM COMPANY_E WHERE local_id = 'c-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(null, cursor.getString(0))
        }
        v12.execSQL(
            "UPDATE COMPANY_E SET logo_ref = 'logos/logo-1.jpg' WHERE local_id = 'c-1'",
        )
        v12.query("SELECT logo_ref FROM COMPANY_E WHERE local_id = 'c-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("logos/logo-1.jpg", cursor.getString(0))
        }
        v12.close()
    }

    companion object {
        private const val DB_NAME = "migration-11-12-test.db"
    }
}
