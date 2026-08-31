package com.example.transportapp.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.database.seed.DemoSeeder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S3: the masters dataset seeds to the §B6 canonical counts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MastersSeederTest {

    private lateinit var database: TransportDatabase
    private lateinit var seeder: DemoSeeder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seeder = DemoSeeder(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `masters seed to the canonical counts`() = runTest {
        seeder.seedIfNeeded()
        val dao = database.mastersDao()
        val companyId = com.example.transportapp.core.common.SeedIds.COMPANY_SHIVSHAKTI

        assertEquals(1284, dao.countParties(companyId))
        assertEquals(96, dao.countStations(companyId))
        assertEquals(141, dao.countRoutes(companyId))
        assertEquals(38, dao.countGoods(companyId))
        assertEquals(22, dao.countVehicles(companyId))
        assertEquals(17, dao.countDrivers(companyId))
        assertEquals(9, dao.countChargeHeads(companyId))
        assertEquals(65, dao.countRateCards(companyId))
        assertEquals(12, dao.getRateRowsForParty(com.example.transportapp.core.common.SeedIds.PARTY_DEEPAK_STEEL).size)
        // hamali + door delivery auto-apply (§3 charge templates); surcharge is off so the
        // T5 charge table shows exactly Freight / Hamali / Door delivery (Design T5 §E).
        assertEquals(2, dao.getAutoChargeHeads(companyId).size)
        // The §3 step-5 company default resolves every booking scope.
        assertEquals(1, dao.getRateCandidates(companyId, partyId = null, routeId = null, goodsId = null).size)
        assertEquals(7, dao.observeDuplicateParties(companyId).first().size)
    }
}
