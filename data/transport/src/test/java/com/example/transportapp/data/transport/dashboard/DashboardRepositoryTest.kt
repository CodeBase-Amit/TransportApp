package com.example.transportapp.data.transport.dashboard

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.entity.TripEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.domain.transport.RoleRank
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S10 (Phase2.md test charter): the §13 tile queries against the §B6 seed, and the
 * role-gating matrix. The seed is the fixture — running services, in transit, unbilled
 * freight and receivable are assertable to the rupee.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: DashboardRepositoryImpl
    private lateinit var billing: com.example.transportapp.data.transport.billing.BillingRepositoryImpl
    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val branch = SeedIds.BRANCH_INDORE
    private val now = System.currentTimeMillis()

    private val sessionRepo = object : SessionRepository {
        override val session = flowOf(
            UserSession(
                userId = "u", name = "Mahesh Patidar", email = DemoSeeder.EMAIL_DEMO_USER, role = "OWNER",
                companyId = company, companyName = "Shivshakti Roadlines", branchId = branch, branchName = "Indore",
            ),
        )

        override suspend fun signOut() {}
    }

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = DashboardRepositoryImpl(database, sessionRepo)
        billing = com.example.transportapp.data.transport.billing.BillingRepositoryImpl(
            database,
            sessionRepo,
            com.example.transportapp.data.transport.numbering.NumberingRepositoryImpl(database, database.numberingDao(), deviceIdProvider = { "TEST1" }),
            com.example.transportapp.data.transport.outbox.OutboxWriter(database.outboxDao()),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `the seeded tiles read true`() = runTest {
        val tiles = repository.load(now)

        assertEquals("nothing dispatched on a fresh seed", 0, tiles.runningServices)
        assertEquals("04188 is the one in-transit bilty", 1, tiles.inTransit)
        assertEquals(12L, tiles.inTransitPackages)
        assertTrue(
            "the oldest unbilled booking is the S9 fixture's 43-day slot",
            tiles.unbilledOldestDays?.let { it in 42L..43L } ?: false,
        )
        assertEquals("unbilled = 21 money cns + 04187 + 04184", 6_155_000L, tiles.unbilledPaise)
        assertEquals("issued bills 00298 + 00311, nothing allocated", 1_176_000L, tiles.receivablePaise)
        assertEquals("44 days is not 90+", 0L, tiles.receivableOver90Paise)
        assertEquals(
            "one Held (SHORTAGE on 04185) and one Returned (reason OTHER on 04183)",
            listOf("SHORTAGE", "OTHER"),
            tiles.exceptions.map { it.reasonCode },
        )
        assertEquals(1, tiles.exceptions.first().count)
    }

    @Test
    fun `dispatching a trip lights up running services`() = runTest {
        database.tripDao().upsertTrip(
            TripEntity(
                local_id = "t-running", server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                series_id = "s", challan_no = "CHL/IND/2627/00999", state = "DISPATCHED",
                vehicle_id = SeedIds.VEHICLE_MH15BK4412, driver_id = SeedIds.DRIVER_GURMEET,
                origin_branch_id = branch, dest_station_id = "seed-station-1", via_stations = null,
                hire_paise = 1_000_000, advance_paise = 0, balance_paise = 1_000_000,
                expected_arrival = now + 26 * 3600_000, created_at = now, created_by_name = "Mahesh",
                dispatched_at = now, closed_at = null, cancel_reason = null,
            ),
        )
        val tiles = repository.load(now)
        assertEquals(1, tiles.runningServices)
        assertEquals(now + 26 * 3600_000, tiles.nearestRunningArrival)
    }

    @Test
    fun `a collected to pay leaves the to-pay tile`() = runTest {
        // 04185 is TOPAY but HELD — the tile counts Arrived / Out for delivery only.
        val collected = insertTopay("IND/2627/90060", status = "ARRIVED")
        var tiles = repository.load(now)
        assertEquals(1, tiles.topayAwaiting)
        assertEquals(105_000L, tiles.topayAwaitingPaise)

        billing.recordReceipt(
            payerPartyId = SeedIds.PARTY_NASHIK_HARDWARE, amountPaise = 105_000,
            instrument = "CASH", instrumentRef = null,
            allocations = listOf(
                com.example.transportapp.data.transport.billing.AllocationInput(
                    targetType = "TOPAY_CONSIGNMENT", consignmentId = collected, amountPaise = 105_000,
                ),
            ),
            now = now,
        )
        tiles = repository.load(now)
        assertEquals("collected money is no longer waiting", 0, tiles.topayAwaiting)
    }

    @Test
    fun `overdue buckets count the grace period`() = runTest {
        // Booked 10 days ago with a 2-day transit: 8 days past expected, ignoring grace.
        insertTopay("IND/2627/90061", status = "BOOKED", bookedDaysAgo = 10)
        val tiles = repository.load(now)
        assertTrue("the 10-day-old booking is 8 days overdue (7+ bucket)", tiles.ageing7plus >= 1)
    }

    @Test
    fun `own vehicles with no open trip are idle`() = runTest {
        val tiles = repository.load(now)
        // 15 own vehicles in the seed, none on a trip: all idle well past the 7-day bar.
        assertTrue(tiles.idleVehicles.size >= 15)
        assertTrue(tiles.idleVehicles.all { it.idleDays >= 7 })
    }

    @Test
    fun `the month tile's margin is freight minus hire minus cost`() = runTest {
        insertTopay("IND/2627/90062", status = "BOOKED", bookedDaysAgo = 0, freightPaise = 500_000)
        val tiles = repository.load(now)
        assertTrue("booked just now lands in this month", tiles.month.freightPaise >= 500_000)
        assertEquals(tiles.month.freightPaise - tiles.month.hirePaise - tiles.month.costPaise, tiles.month.marginPaise)
    }

    @Test
    fun `role gating hides accountant tiles from a clerk`() {
        val repo = repository as DashboardRepository
        assertTrue(repo.tileVisible("OWNER", "ACCOUNTANT"))
        assertTrue(repo.tileVisible("ACCOUNTANT", "ACCOUNTANT"))
        assertFalse("Booking Clerk hides the receivable tile", repo.tileVisible("BOOKING_CLERK", "ACCOUNTANT"))
        assertFalse("Booking Clerk hides the idle-vehicles tile", repo.tileVisible("BOOKING_CLERK", "MANAGER"))
        assertTrue(repo.tileVisible("DELIVERY_CLERK", "DELIVERY_CLERK"))
        assertEquals("the §13 gating matrix agrees with RoleRank", RoleRank.atLeast("MANAGER", "ACCOUNTANT"), repo.tileVisible("MANAGER", "ACCOUNTANT"))
    }

    private suspend fun insertTopay(biltyNo: String, status: String, bookedDaysAgo: Long = 0, freightPaise: Long = 100_000): String {
        val route = database.mastersDao().getRoute(SeedIds.ROUTE_INDORE_NASHIK)!!
        val id = "cn-$biltyNo"
        val gst = freightPaise / 20
        database.consignmentDao().upsertConsignment(
            ConsignmentEntity(
                local_id = id, server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.SYNCED, deleted_at = null, company_id = company,
                series_id = SeedIds.SERIES_BILTY_INDORE, bilty_no = biltyNo, provisional_no = null,
                status_projection = status, booking_branch_id = branch, dest_branch_id = null,
                consignor_id = SeedIds.PARTY_DEEPAK_STEEL, consignee_id = SeedIds.PARTY_NASHIK_HARDWARE,
                route_id = SeedIds.ROUTE_INDORE_NASHIK,
                from_station_id = route.origin_station_id, to_station_id = route.dest_station_id,
                payment_mode = "TOPAY", risk = "OWNERS", delivery_type = "GODOWN",
                place_of_supply_state = "Maharashtra", eway_bill_no = null, private_mark = null,
                packages = 1, actual_weight_g = 1000, chargeable_weight_g = 1000,
                declared_value_paise = 0, freight_paise = freightPaise, gst_paise = gst,
                total_paise = freightPaise + gst,
                booked_at = now - bookedDaysAgo * DashboardRepositoryImpl.DAY_MS, booked_by_name = "Seeder",
                expected_arrival = now - bookedDaysAgo * DashboardRepositoryImpl.DAY_MS + 2 * DashboardRepositoryImpl.DAY_MS,
                party_names = "Deepak Steel Traders; Nashik Hardware Mart",
                freight_bill_id = null, amends_id = null, amendment_reason = null,
            ),
        )
        return id
    }
}
