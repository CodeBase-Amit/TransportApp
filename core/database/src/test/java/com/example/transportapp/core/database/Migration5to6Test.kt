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
 * S7: 5 → 6 adds the trip aggregate (§11): TRIP_E, TRIP_LEG_E, TRIP_COST_E, LORRY_HIRE_E.
 * Proves schema validity, that a trip with legs can be written at v6 (FKs live), and that
 * the leg-unique index actually refuses a duplicate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration5to6Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    private fun insertV5Consignment(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL(
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
                NULL, NULL, NULL,
                12, 780000, 780000, 0,
                351000, 18780, 394400,
                1, 'Mahesh Patidar', 3, 'Deepak Steel Traders; Nashik Hardware Mart',
                NULL, NULL
            )
            """.trimIndent(),
        )
    }

    @Test
    fun `migrate 5 to 6 creates trip tables with working foreign keys`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 5).use { v5 ->
            insertV5Consignment(v5)
        }

        val v6 = helper.runMigrationsAndValidate(dbName, version = 6, validateDroppedTables = true, TransportDatabase.MIGRATION_5_6)

        v6.execSQL(
            """
            INSERT INTO TRIP_E (
                local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id,
                series_id, challan_no, state, vehicle_id, driver_id, origin_branch_id, dest_station_id,
                via_stations, hire_paise, advance_paise, balance_paise, expected_arrival,
                created_at, created_by_name, dispatched_at, closed_at, cancel_reason
            ) VALUES (
                't-1', NULL, 1, NULL, 'SYNCED', NULL, 'c-1',
                'cs-1', 'CHL/IND/2627/00742', 'ISSUED', 'v-1', 'd-1', 'b-1', 'st-9',
                NULL, 1850000, 1200000, 650000, 10,
                1, 'Mahesh Patidar', NULL, NULL, NULL
            )
            """.trimIndent(),
        )
        v6.execSQL(
            "INSERT INTO TRIP_LEG_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, trip_id, consignment_id, leg_no, loaded_at) " +
                "VALUES ('tl-1', NULL, 1, NULL, 'SYNCED', NULL, 't-1', 'cn-1', 1, 1)",
        )
        v6.query("SELECT COUNT(*) FROM TRIP_LEG_E").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the leg's FKs resolve to the trip and the v5 consignment", 1, cursor.getInt(0))
        }
        v6.execSQL(
            "INSERT INTO LORRY_HIRE_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, trip_id, owner_party_id, broker_id, hire_paise, advance_paise, deductions_paise, balance_paise) " +
                "VALUES ('lh-1', NULL, 1, NULL, 'SYNCED', NULL, 't-1', NULL, NULL, 1850000, 1200000, 0, 650000)",
        )
        var refused = false
        try {
            v6.execSQL(
                "INSERT INTO TRIP_LEG_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, trip_id, consignment_id, leg_no, loaded_at) " +
                    "VALUES ('tl-2', NULL, 2, NULL, 'SYNCED', NULL, 't-1', 'cn-1', 2, 2)",
            )
        } catch (expected: Exception) {
            refused = true
        }
        assertTrue("the (trip, consignment) unique index refuses a duplicate leg", refused)
        v6.close()
    }

    companion object {
        private const val DB_NAME = "migration-5-6-test.db"
    }
}
