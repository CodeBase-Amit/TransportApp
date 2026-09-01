package com.example.transportapp.data.transport.rate

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.CompanySettingEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.domain.transport.calc.GstTreatment
import com.example.transportapp.domain.transport.calc.RoundingRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S14: the dated settings behaviour (§10.5). A settings row inserted with a newer
 * effective_from governs new bookings (GST 12% variant prices correctly), while
 * already-booked bilties keep the figures frozen in their charge lines at booking time —
 * the §12.1 freeze principle applied to configuration.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatedSettingsTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: RateCardRepositoryImpl
    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = RateCardRepositoryImpl(database.mastersDao(), database.orgDao(), database.settingsDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertSetting(localId: String, effectiveFrom: Long, gstRateBp: Int, divisor: Long?) {
        database.settingsDao().upsertSetting(
            CompanySettingEntity(
                local_id = localId, server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                effective_from = effectiveFrom, gst_rate_bp = gstRateBp, weight_step_g = 1000,
                volumetric_divisor_g = divisor, gst_treatment = "FORWARD",
                rounding = "NEAREST_RUPEE", created_by_name = "Mahesh Patidar",
            ),
        )
    }

    @Test
    fun `the seeded setting governs before any change`() = runTest {
        val settings = repository.bookingSettings(company, SeedIds.ROUTE_INDORE_NASHIK)
        assertEquals(500, settings.gstRateBp)
        assertEquals(6000L, settings.volumetricDivisor)
    }

    @Test
    fun `a newer effective_from governs new bookings — the GST 12 percent variant`() = runTest {
        insertSetting("setting-v2-gst12", effectiveFrom = now - 1000, gstRateBp = 1200, divisor = 6000)

        val settings = repository.bookingSettings(company, SeedIds.ROUTE_INDORE_NASHIK)
        assertEquals("the newest governing row wins", 1200, settings.gstRateBp)

        // And the engine prices the canonical 780 kg booking at 12%: the taxable base is
        // freight 3,510.00 + hamali 96.00 + door 150.00 = 3,756.00 → GST 45,072 paise
        // (the §10.6 fixture's taxable 375,600 × 12%).
        val input = com.example.transportapp.domain.transport.calc.CalculationInput(
            packages = 12, actualWeightG = 780_000,
            weightStepG = settings.weightStepG, volumetricDivisor = settings.volumetricDivisor,
            heads = repository.autoApplyHeads(company),
            rate = repository.resolveBookingRate(company, SeedIds.PARTY_DEEPAK_STEEL, SeedIds.ROUTE_INDORE_NASHIK, SeedIds.GOODS_MS_PIPES),
            gst = com.example.transportapp.domain.transport.calc.GstConfig(
                treatment = settings.gstTreatment, rateBp = settings.gstRateBp,
                placeOfSupplyState = settings.defaultPlaceOfSupplyState,
                companyRegisteredState = settings.companyRegisteredState,
            ),
            rounding = settings.rounding,
        )
        val result = com.example.transportapp.domain.transport.calc.ChargeCalculator.calculate(input)
        assertEquals("GST line at 12% of taxable 375,600", 45_072L, result.gst?.totalPaise)
    }

    @Test
    fun `a future effective_from does not govern yet`() = runTest {
        insertSetting("setting-future", effectiveFrom = now + 30L * 24 * 60 * 60 * 1000, gstRateBp = 1800, divisor = 5000)
        val settings = repository.bookingSettings(company, SeedIds.ROUTE_INDORE_NASHIK)
        assertEquals("the future row waits", 500, settings.gstRateBp)
    }

    @Test
    fun `already-booked bilties keep their frozen charge lines across a setting change`() = runTest {
        // The seeded 04188 carries the §10.6 fixture at 5%: freight 351,000 + gst 18,780.
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(company, SeedIds.BILTY_04188)!!
        val lines = database.consignmentDao().getChargeLines(consignment.local_id)
        val gstLine = lines.first { it.head_code == "gst" }
        assertEquals("the frozen 5% GST line", 18_780L, gstLine.computed_paise)

        // Change the setting to 12%: the stored bilty is untouched, new bookings price 12%.
        insertSetting("setting-v2-gst12", effectiveFrom = now - 1000, gstRateBp = 1200, divisor = 6000)
        val linesAfter = database.consignmentDao().getChargeLines(consignment.local_id)
        assertEquals(18_780L, linesAfter.first { it.head_code == "gst" }.computed_paise)
        assertEquals(1200, repository.bookingSettings(company, SeedIds.ROUTE_INDORE_NASHIK).gstRateBp)
        assertTrue(linesAfter.size == lines.size)
    }

    @Test
    fun `a null divisor turns volumetric off again — the dated full-load house`() = runTest {
        insertSetting("setting-v3-novol", effectiveFrom = now - 1000, gstRateBp = 500, divisor = null)
        val settings = repository.bookingSettings(company, SeedIds.ROUTE_INDORE_NASHIK)
        assertEquals(null, settings.volumetricDivisor)
        assertEquals(RoundingRule.NEAREST_RUPEE, settings.rounding)
    }
}
