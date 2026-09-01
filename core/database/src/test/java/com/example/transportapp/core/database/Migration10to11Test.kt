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
 * S15: 10 → 11 adds CONSIGNMENT_E.amendment_reason (§16.1) — an amendment is another
 * consignment row with its reason carried on the amendment itself. The column is nullable:
 * the seeded rows and every pre-S15 booking survive untouched.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration10to11Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 10 to 11 adds amendment_reason and preserves rows`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 10).use { v10 ->
            v10.execSQL(
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
                    'c-1', 's-1', 'IND/2627/04188', NULL, 'BOOKED',
                    'b-1', NULL, 'p-1', 'p-2', 'r-1',
                    'st-1', 'st-2', 'TOPAY', 'OWNERS', 'DOOR',
                    NULL, NULL, NULL,
                    12, 780000, 780000, 0,
                    351000, 18780, 394400,
                    1, 'Mahesh Patidar', 3, 'Deepak Steel Traders; Nashik Hardware Mart',
                    NULL, NULL
                )
                """.trimIndent(),
            )
        }

        val v11 = helper.runMigrationsAndValidate(dbName, version = 11, validateDroppedTables = true, TransportDatabase.MIGRATION_10_11)

        // The pre-migration row survives with a null reason; a new amendment row carries one.
        v11.query("SELECT amendment_reason FROM CONSIGNMENT_E WHERE local_id = 'cn-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(null, cursor.getString(0))
        }
        v11.execSQL(
            "INSERT INTO CONSIGNMENT_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id, series_id, bilty_no, provisional_no, status_projection, booking_branch_id, dest_branch_id, consignor_id, consignee_id, route_id, from_station_id, to_station_id, payment_mode, risk, delivery_type, place_of_supply_state, eway_bill_no, private_mark, packages, actual_weight_g, chargeable_weight_g, declared_value_paise, freight_paise, gst_paise, total_paise, booked_at, booked_by_name, expected_arrival, party_names, freight_bill_id, amends_id, amendment_reason) " +
                "VALUES ('cn-2', NULL, 2, NULL, 'PENDING', NULL, 'c-1', 's-1', 'IND/2627/04201', NULL, 'BOOKED', 'b-1', NULL, 'p-1', 'p-2', 'r-1', 'st-1', 'st-2', 'TOPAY', 'OWNERS', 'DOOR', NULL, NULL, NULL, 12, 780000, 780000, 0, 351000, 18780, 394400, 2, 'Mahesh Patidar', 4, 'Deepak Steel Traders; Nashik Hardware Mart', NULL, 'cn-1', 'Weight corrected at loading')",
        )
        v11.query("SELECT amendment_reason, amends_id FROM CONSIGNMENT_E WHERE local_id = 'cn-2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Weight corrected at loading", cursor.getString(0))
            assertEquals("cn-1", cursor.getString(1))
        }
        v11.close()
    }

    companion object {
        private const val DB_NAME = "migration-10-11-test.db"
    }
}
