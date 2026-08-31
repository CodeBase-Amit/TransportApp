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
 * S5: 4 → 5 adds numbering (§9) and the consignment aggregate (§16.1) + CONSIGNMENT_FTS.
 * Proves (a) schema validity after migration and (b) the FTS content-sync triggers work on
 * a migrated database — a consignment inserted at v4 must be searchable through
 * CONSIGNMENT_FTS at v5.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration4to5Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 4 to 5 creates numbering and consignment tables and indexes new consignments`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 4).close()

        val v5 = helper.runMigrationsAndValidate(dbName, version = 5, validateDroppedTables = true, TransportDatabase.MIGRATION_4_5)

        v5.execSQL(
            """
            INSERT INTO CONSIGNMENT_E (
                local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at,
                company_id, series_id, bilty_no, provisional_no, status_projection,
                booking_branch_id, dest_branch_id, consignor_id, consignee_id, route_id,
                from_station_id, to_station_id, payment_mode, risk, delivery_type,
                place_of_supply_state, eway_bill_no, private_mark,
                packages, actual_weight_g, chargeable_weight_g, declared_value_paise,
                freight_paise, gst_paise, total_paise,
                booked_at, booked_by_name, expected_arrival, party_names,
                freight_bill_id, amends_id
            ) VALUES (
                'cn-1', NULL, 1, NULL, 'SYNCED', NULL,
                'c-1', 's-1', 'IND/2627/04189', NULL, 'BOOKED',
                'b-1', NULL, 'p-1', 'p-2', 'r-1',
                'st-1', 'st-2', 'TOPAY', 'OWNERS', 'DOOR',
                'Maharashtra', NULL, NULL,
                12, 780000, 780000, 0,
                351000, 18780, 394400,
                1, 'Mahesh Patidar', 3, 'Deepak Steel Traders; Nashik Hardware Mart',
                NULL, NULL
            )
            """.trimIndent(),
        )
        v5.query("SELECT COUNT(*) FROM CONSIGNMENT_FTS").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the migrated FTS trigger indexed the new consignment", 1, cursor.getInt(0))
        }
        v5.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('NUMBER_SERIES_E','NUMBER_LEASE_E','DOC_SNAPSHOT_E','CHARGE_LINE_E','STATUS_EVENT_E','CONSIGNMENT_ITEM_E')").use { cursor ->
            var seen = 0
            while (cursor.moveToNext()) seen++
            assertEquals(6, seen)
        }
        v5.execSQL("INSERT INTO NUMBER_SERIES_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id, branch_id, doc_type, prefix, fy_part, digits, last_issued, reset_rule) VALUES ('s-1', NULL, 1, NULL, 'SYNCED', NULL, 'c-1', 'b-1', 'BILTY', 'IND/2627/', '2627', 5, 4188, 'FINANCIAL_YEARLY')")
        v5.query("SELECT last_issued FROM NUMBER_SERIES_E WHERE local_id = 's-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(4188L, cursor.getLong(0))
        }
        v5.close()
    }

    companion object {
        private const val DB_NAME = "migration-4-5-test.db"
    }
}
