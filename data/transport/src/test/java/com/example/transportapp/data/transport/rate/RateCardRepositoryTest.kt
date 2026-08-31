package com.example.transportapp.data.transport.rate

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.entity.RateCardEntity
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.domain.transport.calc.ChargeHeadBasis
import com.example.transportapp.domain.transport.calc.GstTreatment
import com.example.transportapp.domain.transport.calc.RateBasis
import com.example.transportapp.domain.transport.calc.RoundingRule
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S4: the read path behind the calculator — resolution over seeded masters, §3 order. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RateCardRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: RateCardRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = RateCardRepositoryImpl(database.mastersDao(), database.orgDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val company = SeedIds.COMPANY_SHIVSHAKTI

    @Test
    fun `canonical row resolves at step 1 from the party rate card`() = runTest {
        val resolved = repository.resolveBookingRate(
            company,
            partyId = SeedIds.PARTY_DEEPAK_STEEL,
            routeId = SeedIds.ROUTE_INDORE_NASHIK,
            goodsId = SeedIds.GOODS_MS_PIPES,
        )!!
        assertEquals(1, resolved.step)
        assertEquals(RateBasis.PER_KG, resolved.candidate.basis)
        assertEquals(450L, resolved.candidate.ratePaise)
        assertEquals("500 kg", resolved.candidate.minQtyLabel)
    }

    @Test
    fun `a party without a card falls back to the company default`() = runTest {
        val resolved = repository.resolveBookingRate(
            company,
            partyId = SeedIds.PARTY_NASHIK_HARDWARE,
            routeId = SeedIds.ROUTE_INDORE_NASHIK,
            goodsId = SeedIds.GOODS_MS_PIPES,
        )!!
        assertEquals(5, resolved.step)
        assertEquals("seed-rate-co-default", resolved.candidate.localId)
        assertEquals(450L, resolved.candidate.ratePaise)
    }

    @Test
    fun `rows with an unknown basis token are skipped, not fatal`() = runTest {
        database.mastersDao().upsertRateCard(
            RateCardEntity(
                local_id = "test-rate-weird", server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                party_id = SeedIds.PARTY_DEEPAK_STEEL, route_id = SeedIds.ROUTE_INDORE_NASHIK, goods_id = SeedIds.GOODS_MS_PIPES,
                basis = "WEIRD", rate_paise = 1, min_qty_label = null, min_freight_paise = null,
                max_freight_paise = null, note = null, sort_order = -1,
            ),
        )
        val resolved = repository.resolveBookingRate(
            company,
            partyId = SeedIds.PARTY_DEEPAK_STEEL,
            routeId = SeedIds.ROUTE_INDORE_NASHIK,
            goodsId = SeedIds.GOODS_MS_PIPES,
        )!!
        assertEquals(1, resolved.step)
        assertEquals(450L, resolved.candidate.ratePaise)
    }

    @Test
    fun `freight bounds and auto heads flow through`() = runTest {
        database.mastersDao().upsertRateCard(
            RateCardEntity(
                local_id = "test-rate-bounds", server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                party_id = SeedIds.PARTY_DEEPAK_STEEL, route_id = SeedIds.ROUTE_INDORE_NASHIK, goods_id = null,
                basis = "PER_KG", rate_paise = 500, min_qty_label = null, min_freight_paise = 35_000,
                max_freight_paise = 400_000, note = null, sort_order = 0,
            ),
        )
        val withoutGoods = repository.resolveBookingRate(
            company,
            partyId = SeedIds.PARTY_DEEPAK_STEEL,
            routeId = SeedIds.ROUTE_INDORE_NASHIK,
            goodsId = null,
        )!!
        assertEquals(2, withoutGoods.step)
        assertEquals(35_000L, withoutGoods.candidate.minFreightPaise)
        assertEquals(400_000L, withoutGoods.candidate.maxFreightPaise)

        val heads = repository.autoApplyHeads(company)
        assertEquals(listOf("hamali", "door_delivery"), heads.map { it.code })
        assertEquals(ChargeHeadBasis.PER_PACKAGE, heads[0].basis)
        assertEquals(800L, heads[0].defaultValue)
        assertEquals(ChargeHeadBasis.FLAT, heads[1].basis)
        assertEquals(15_000L, heads[1].defaultValue)
    }

    @Test
    fun `booking settings carry the 10-5 inputs`() = runTest {
        val settings = repository.bookingSettings(company, routeId = SeedIds.ROUTE_INDORE_NASHIK)
        assertEquals(1000L, settings.weightStepG)
        assertNull("full-load house: volumetric off", settings.volumetricDivisor)
        assertEquals(GstTreatment.FORWARD, settings.gstTreatment)
        assertEquals(500, settings.gstRateBp)
        assertEquals(RoundingRule.NEAREST_RUPEE, settings.rounding)
        assertEquals("Madhya Pradesh", settings.companyRegisteredState)
        assertEquals("place of supply defaults to the destination station's state", "Maharashtra", settings.defaultPlaceOfSupplyState)
    }
}
