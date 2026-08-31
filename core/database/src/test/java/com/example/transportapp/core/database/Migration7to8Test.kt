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
 * S9: 7 → 8 adds the money tables (§12): FREIGHT_BILL_E, CREDIT_NOTE_E, RECEIPT_E,
 * RECEIPT_ALLOCATION_E. Proves schema validity, that a bill and a receipt with explicit
 * allocations can be written at v8, and that the (company, receipt_no) unique index refuses
 * a duplicate receipt number.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration7to8Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 7 to 8 creates money tables with unique receipt numbers`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 7).use { v7 ->
            v7.execSQL(
                """
                INSERT INTO PARTY_E (
                    local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at,
                    company_id, name, phone, email, type, street_address, station, pincode, gstin,
                    usual_route_id, usual_payment_mode, display_bilty_count
                ) VALUES (
                    'p-1', NULL, 1, NULL, 'SYNCED', NULL,
                    'c-1', 'Deepak Steel Traders', '+91 94250 61183', NULL, 'BOTH', NULL, NULL, NULL, NULL,
                    NULL, NULL, 0
                )
                """.trimIndent(),
            )
        }

        val v8 = helper.runMigrationsAndValidate(dbName, version = 8, validateDroppedTables = true, TransportDatabase.MIGRATION_7_8)

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
        v8.execSQL(
            """
            INSERT INTO RECEIPT_E (
                local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id,
                series_id, receipt_no, party_id, amount_paise, instrument, instrument_ref,
                received_at, received_at_branch_id, received_by_name, notes
            ) VALUES (
                'r-1', NULL, 1, NULL, 'SYNCED', NULL, 'c-1',
                'cs-1', 'RCPT/IND/2627/00129', 'p-1', 5000000, 'NEFT', 'SBIN0026412188',
                4, 'b-1', 'Mahesh Patidar', NULL
            )
            """.trimIndent(),
        )
        v8.execSQL(
            "INSERT INTO RECEIPT_ALLOCATION_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id, receipt_id, target_type, bill_id, consignment_id, amount_paise) " +
                "VALUES ('ra-1', NULL, 1, NULL, 'SYNCED', NULL, 'c-1', 'r-1', 'BILL', 'fb-1', NULL, 5000000)",
        )
        v8.query("SELECT SUM(amount_paise) FROM RECEIPT_ALLOCATION_E WHERE bill_id = 'fb-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the allocation lands on the bill", 5000000L, cursor.getLong(0))
        }

        var refused = false
        try {
            v8.execSQL(
                "INSERT INTO RECEIPT_E (local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at, company_id, series_id, receipt_no, party_id, amount_paise, instrument, instrument_ref, received_at, received_at_branch_id, received_by_name, notes) " +
                    "VALUES ('r-2', NULL, 2, NULL, 'SYNCED', NULL, 'c-1', 'cs-1', 'RCPT/IND/2627/00129', 'p-1', 100, 'CASH', NULL, 5, 'b-1', 'Mahesh Patidar', NULL)",
            )
        } catch (expected: Exception) {
            refused = true
        }
        assertTrue("the (company, receipt_no) unique index refuses a duplicate receipt number", refused)
        v8.close()
    }

    companion object {
        private const val DB_NAME = "migration-7-8-test.db"
    }
}
