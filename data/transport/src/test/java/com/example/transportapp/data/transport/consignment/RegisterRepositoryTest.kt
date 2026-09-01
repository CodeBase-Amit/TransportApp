package com.example.transportapp.data.transport.consignment

import androidx.paging.PagingConfig
import androidx.room.Room
import androidx.paging.testing.asSnapshot
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
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
 * S6 (Phase2.md test charter): register filters vs hand-computed SQL on the §B6 seed, the
 * summary-strip aggregates, and a 10,000-row paging boundary fixture — every row loaded
 * exactly once, none skipped, none duplicated.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RegisterRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: RegisterRepositoryImpl

    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val branch = SeedIds.BRANCH_INDORE

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = RegisterRepositoryImpl(database.consignmentDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun noFilter() = RegisterFilter()

    private suspend fun numbers(filter: RegisterFilter): List<String> =
        repository.pagingRegister(company, branch, filter)
            .asSnapshot()
            .map { it.displayNo }

    @Test
    fun `no filter returns the indore seed newest first`() = runTest {
        // The default scope is the active branch; 04184 books at Nagpur.
        assertEquals(
            listOf("IND/2627/04188", "IND/2627/04187", "IND/2627/04186", "IND/2627/04185", "IND/2627/04183"),
            numbers(noFilter()),
        )
    }

    @Test
    fun `status filters match exactly one seeded row each`() = runTest {
        val allBranches = RegisterFilter(allBranches = true)
        assertEquals(listOf("IND/2627/04188"), numbers(RegisterFilter(allBranches = true, status = ConsignmentStatus.IN_TRANSIT)))
        assertEquals(listOf("IND/2627/04185"), numbers(RegisterFilter(allBranches = true, status = ConsignmentStatus.HELD)))
        assertEquals(listOf("IND/2627/04186"), numbers(RegisterFilter(allBranches = true, status = ConsignmentStatus.DELIVERED)))
        assertEquals(listOf("IND/2627/04184"), numbers(RegisterFilter(allBranches = true, status = ConsignmentStatus.OUT_FOR_DELIVERY)))
        assertEquals(listOf("IND/2627/04183"), numbers(RegisterFilter(allBranches = true, status = ConsignmentStatus.RETURNED)))
        assertEquals(listOf("IND/2627/04187"), numbers(RegisterFilter(allBranches = true, status = ConsignmentStatus.AT_HUB)))
        assertEquals(
            "six fixture statuses plus the S9 money pool booked at Nagpur",
            35,
            numbers(allBranches).size,
        )
    }

    @Test
    fun `to pay matches the three topay bilties`() = runTest {
        assertEquals(
            listOf("IND/2627/04188", "IND/2627/04185", "IND/2627/04183"),
            numbers(RegisterFilter(paymentMode = PaymentMode.TOPAY)),
        )
    }

    @Test
    fun `branch scope excludes the nagpur booking`() = runTest {
        val active = numbers(noFilter())
        val all = numbers(RegisterFilter(allBranches = true))
        assertEquals("the six fixtures plus the S9 money pool", 35, all.size)
        assertEquals(5, active.size)
        assertTrue(!active.contains("IND/2627/04184"))
        assertTrue(all.contains("IND/2627/04184"))
    }

    @Test
    fun `held rows carry the held remark`() = runTest {
        val rows = repository.pagingRegister(company, branch, RegisterFilter(status = ConsignmentStatus.HELD))
            .asSnapshot()
        assertEquals(1, rows.size)
        assertTrue(rows[0].heldRemark!!.startsWith("Shortage - 1 bundle short"))
    }

    @Test
    fun `search matches number and party denorm via bounded like`() = runTest {
        assertEquals("number search", listOf("IND/2627/04186"), numbers(RegisterFilter(search = "04186")))
        assertEquals(
            "party search hits both Nashik Hardware rows",
            listOf("IND/2627/04188", "IND/2627/04185"),
            numbers(RegisterFilter(search = "Nashik Hardware")),
        )
        assertTrue("no match for garbage", numbers(RegisterFilter(search = "zzzz")).isEmpty())
    }

    @Test
    fun `summary aggregates match hand-computed sums`() = runTest {
        val all = repository.summary(company, branch, noFilter())
        assertEquals("matching (indore rows)", 5, all.matching)
        assertEquals("packages 12+40+8+6+9", 75L, all.packages)
        assertEquals("amounts 3944+12180+6750+2410+4060 rupees", 2_934_400L, all.amountPaise)

        val toPay = repository.summary(company, branch, RegisterFilter(paymentMode = PaymentMode.TOPAY))
        assertEquals(3, toPay.matching)
        assertEquals(27L, toPay.packages)
        assertEquals(1_041_400L, toPay.amountPaise)

        val held = repository.summary(company, branch, RegisterFilter(status = ConsignmentStatus.HELD))
        assertEquals(1, held.matching)
        assertEquals(241_000L, held.amountPaise)
    }

    @Test
    fun `paging loads ten thousand rows exactly once each`() = runTest {
        // 10,000 extra rows on an old party/station, each a distinct booked_at so the
        // ordering is strict — the boundary fixture proves no dup and no skip.
        val consignor = SeedIds.PARTY_DEEPAK_STEEL
        val consignee = SeedIds.PARTY_NASHIK_HARDWARE
        val route = database.mastersDao().getRoute(SeedIds.ROUTE_INDORE_NASHIK)!!
        val base = 1_000_000_000L
        for (i in 0 until 10_000) {
            database.consignmentDao().upsertConsignment(
                ConsignmentEntity(
                    local_id = "gen-cn-$i", server_id = null, updated_at_local = 1, updated_at_server = null,
                    sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                    series_id = SeedIds.SERIES_BILTY_INDORE, bilty_no = "IND/2627/%05d".format(10_000 + i),
                    provisional_no = null, status_projection = "BOOKED",
                    booking_branch_id = branch, dest_branch_id = null,
                    consignor_id = consignor, consignee_id = consignee, route_id = SeedIds.ROUTE_INDORE_NASHIK,
                    from_station_id = route.origin_station_id, to_station_id = route.dest_station_id,
                    payment_mode = "TOPAY", risk = "OWNERS", delivery_type = "GODOWN",
                    place_of_supply_state = null, eway_bill_no = null, private_mark = null,
                    packages = 1, actual_weight_g = 1000, chargeable_weight_g = 1000, declared_value_paise = 0,
                    freight_paise = 100, gst_paise = 5, total_paise = 105,
                    booked_at = base + i, booked_by_name = "Seeder",
                    expected_arrival = base + i, party_names = "Deepak Steel Traders; Nashik Hardware Mart",
                    freight_bill_id = null, amends_id = null, amendment_reason = null,
                ),
            )
        }

        assertEquals("summary counts all 10,035 across branches (fixtures + money pool + 10k)", 10_035, repository.summary(company, branch, RegisterFilter(allBranches = true)).matching)

        val loaded = repository.pagingRegister(company, branch, RegisterFilter(allBranches = true))
            .asSnapshot { appendScrollWhile { true } }
        assertEquals("every row loaded exactly once", 10_035, loaded.size)
        assertEquals("no duplicates", loaded.size, loaded.map { it.localId }.toSet().size)
        assertEquals("newest first", loaded.maxOf { it.bookedAt }, loaded.first().bookedAt)
        assertTrue("the seeded fixtures are in the newest page", loaded.take(10).any { it.displayNo == "IND/2627/04188" })
    }
}
