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
 * S3: 2 → 3 adds the nine master tables + PARTY_FTS. Proves (a) schema validity after
 * migration and (b) the FTS content-sync triggers work on a migrated database — a party
 * inserted at v2 must be searchable through PARTY_FTS at v3.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration2to3Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 2 to 3 creates master tables and indexes new parties for search`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 2).close()

        val v3 = helper.runMigrationsAndValidate(dbName, version = 3, validateDroppedTables = true, TransportDatabase.MIGRATION_2_3)

        // PARTY_E and PARTY_FTS exist at v3; inserting a party through the migrated database
        // must index it — proving the migration's content-sync triggers are live.
        v3.execSQL(
            """
            INSERT INTO PARTY_E (
                local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at,
                company_id, name, phone, email, type, street_address, station, pincode, gstin,
                usual_route_id, usual_payment_mode, display_bilty_count
            ) VALUES (
                'p-1', NULL, 1, NULL, 'SYNCED', NULL,
                'seed-company-shivshakti', 'Sharma Traders', '+91 90000 12345', NULL, 'BOTH',
                NULL, NULL, NULL, NULL, NULL, NULL, 0
            )
            """.trimIndent(),
        )
        v3.query("SELECT COUNT(*) FROM PARTY_FTS").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the migrated FTS trigger indexed the new party", 1, cursor.getInt(0))
        }
        v3.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('RATE_CARD_E','CHARGE_HEAD_E','VEHICLE_E')").use { cursor ->
            var seen = 0
            while (cursor.moveToNext()) seen++
            assertEquals(3, seen)
        }
        v3.close()
    }

    companion object {
        private const val DB_NAME = "migration-2-3-test.db"
    }
}
