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
 * S4: 3 → 4 adds the calculation engine's columns (Phase2.md §3.2): CHARGE_HEAD_E gains
 * default_value_paise and bearer, RATE_CARD_E gains min/max freight. Proves (a) schema
 * validity after migration, (b) pre-migration rows read back with the neutral defaults,
 * (c) new columns are writable at v4.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration3to4Test {

    private val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TransportDatabase::class.java,
    )

    @Test
    fun `migrate 3 to 4 adds head defaults and rate freight bounds`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = context.getDatabasePath(DB_NAME).absolutePath

        helper.createDatabase(dbName, version = 3).use { v3 ->
            // A v3-shaped charge head and rate row, written without the new columns.
            v3.execSQL(
                """
                INSERT INTO CHARGE_HEAD_E (
                    local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at,
                    company_id, code, label, basis, display_value, taxable, auto_apply, sort_order
                ) VALUES (
                    'head-1', NULL, 1, NULL, 'SYNCED', NULL,
                    'c-1', 'hamali', 'Hamali', 'flat', NULL, 1, 1, 0
                )
                """.trimIndent(),
            )
            v3.execSQL(
                """
                INSERT INTO RATE_CARD_E (
                    local_id, server_id, updated_at_local, updated_at_server, sync_state, deleted_at,
                    company_id, party_id, route_id, goods_id, basis, rate_paise,
                    min_qty_label, note, sort_order
                ) VALUES (
                    'rate-1', NULL, 1, NULL, 'SYNCED', NULL,
                    'c-1', NULL, NULL, NULL, 'PER_KG', 450, '500 kg', NULL, 0
                )
                """.trimIndent(),
            )
        }

        val v4 = helper.runMigrationsAndValidate(dbName, version = 4, validateDroppedTables = true, TransportDatabase.MIGRATION_3_4)

        v4.query("SELECT default_value_paise, bearer FROM CHARGE_HEAD_E WHERE local_id = 'head-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("pre-migration head gains the neutral default value", 0L, cursor.getLong(0))
            assertEquals("pre-migration head gains the default bearer", "CONSIGNOR", cursor.getString(1))
        }
        v4.query("SELECT min_freight_paise, max_freight_paise FROM RATE_CARD_E WHERE local_id = 'rate-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue("freight bounds default to unset", cursor.isNull(0) && cursor.isNull(1))
        }
        v4.execSQL(
            "UPDATE RATE_CARD_E SET min_freight_paise = 35000, max_freight_paise = NULL WHERE local_id = 'rate-1'",
        )
        v4.query("SELECT min_freight_paise FROM RATE_CARD_E WHERE local_id = 'rate-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("freight bounds are writable at v4", 35000L, cursor.getLong(0))
        }
        v4.close()
    }

    companion object {
        private const val DB_NAME = "migration-3-4-test.db"
    }
}
