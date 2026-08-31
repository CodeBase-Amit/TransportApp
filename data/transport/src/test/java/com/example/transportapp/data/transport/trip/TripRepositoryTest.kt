package com.example.transportapp.data.transport.trip

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.numbering.NumberingRepositoryImpl
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S7 (Phase2.md test charter): the trip lifecycle with its §11 guards — capacity with the
 * Manager override, the one-open-trip-per-vehicle rule, and the §11.2 close rule that
 * splits arrivals from onward-leg hubs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: TripRepositoryImpl

    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val branch = SeedIds.BRANCH_INDORE
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        val numbering = NumberingRepositoryImpl(database, database.numberingDao(), deviceIdProvider = { "TEST1" })
        repository = TripRepositoryImpl(
            database = database,
            tripDao = database.tripDao(),
            sessionRepository = fakeSession(),
            numberingRepository = numbering,
            outboxWriter = OutboxWriter(database.outboxDao()),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun fakeSession() = object : SessionRepository {
        override val session: Flow<UserSession> = flowOf(
            UserSession(
                userId = "u", name = "Mahesh Patidar", email = "mahesh.patidar@gmail.com", role = "OWNER",
                companyId = company, companyName = "Shivshakti Roadlines",
                branchId = branch, branchName = "Indore",
            ),
        )

        override suspend fun signOut() {}
    }

    private suspend fun insertBooked(biltyNo: String, toStationName: String, weightKg: Long): String {
        val route = database.mastersDao().getRoute(SeedIds.ROUTE_INDORE_NASHIK)!!
        val destId = when (toStationName) {
            "Nashik" -> route.dest_station_id
            else -> "seed-station-2" // Dhule in the §B6 named-station ordering
        }
        val id = "cn-$biltyNo"
        database.consignmentDao().upsertConsignment(
            ConsignmentEntity(
                local_id = id, server_id = null, updated_at_local = 1, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null, company_id = company,
                series_id = SeedIds.SERIES_BILTY_INDORE, bilty_no = biltyNo, provisional_no = null,
                status_projection = "BOOKED", booking_branch_id = branch, dest_branch_id = null,
                consignor_id = SeedIds.PARTY_DEEPAK_STEEL, consignee_id = SeedIds.PARTY_NASHIK_HARDWARE,
                route_id = SeedIds.ROUTE_INDORE_NASHIK,
                from_station_id = route.origin_station_id, to_station_id = destId,
                payment_mode = "TOPAY", risk = "OWNERS", delivery_type = "GODOWN",
                place_of_supply_state = null, eway_bill_no = null, private_mark = null,
                packages = 1, actual_weight_g = weightKg * 1000, chargeable_weight_g = weightKg * 1000,
                declared_value_paise = 0, freight_paise = 100, gst_paise = 5, total_paise = 105,
                booked_at = 1, booked_by_name = "Seeder", expected_arrival = 2,
                party_names = "Deepak Steel Traders; Nashik Hardware Mart",
                freight_bill_id = null, amends_id = null,
            ),
        )
        return id
    }

    private suspend fun pool(): List<LoadableConsignment> =
        repository.observeLoadablePool(company, branch).first()

    @Test
    fun `pool lists booked and at-hub consignments and excludes the rest`() = runTest {
        // Fresh seed: only 04187 (At hub at Indore) is loadable; IN_TRANSIT/DELIVERED/HELD/RETURNED are not.
        val before = pool()
        assertEquals(listOf("IND/2627/04187"), before.map { it.displayNo })

        val nashik = insertBooked("IND/2627/90001", "Nashik", 700)
        insertBooked("IND/2627/90002", "Dhule", 300)

        val rows = pool()
        assertEquals(3, rows.size)
        assertTrue(rows.map { it.displayNo }.containsAll(listOf("IND/2627/04187", "IND/2627/90001", "IND/2627/90002")))
        assertEquals(nashik, rows.first { it.displayNo == "IND/2627/90001" }.localId)
    }

    @Test
    fun `create and issue stamp the next challan and write Loaded events`() = runTest {
        val nashik = insertBooked("IND/2627/90001", "Nashik", 700)
        val dhule = insertBooked("IND/2627/90002", "Dhule", 300)
        val outboxBefore = database.outboxDao().getPendingCount()

        val trip = repository.createTrip(
            CreateTripDraft(
                vehicleId = SeedIds.VEHICLE_MH15BK4412,
                driverId = SeedIds.DRIVER_GURMEET,
                originBranchId = branch,
                destStationId = "seed-station-1", // Nashik
                viaStationIds = listOf("seed-station-2"),
                consignmentIds = listOf(nashik, dhule),
                hirePaise = 1_850_000,
                advancePaise = 1_200_000,
            ),
            now,
        ).getOrNull()!!

        assertNull("a trip being built carries no challan number (§11.1)", trip.challanNo)

        val issued = repository.issue(trip.tripLocalId, now + 1).getOrNull()!!
        assertEquals("the seeded challan series continues at 742", "CHL/IND/2627/00742", issued.challanNo)

        val reloaded = database.tripDao().getTrip(trip.tripLocalId)!!
        assertEquals("ISSUED", reloaded.state)
        assertEquals("balance = hire − advance", 650_000L, reloaded.balance_paise)
        assertEquals("one Loaded event per consignment", 1, database.consignmentDao().getEvents(nashik).count { it.event_type == "LOADED" })
        assertEquals("one outbox row per consignment event", outboxBefore + 2, database.outboxDao().getPendingCount())
    }

    @Test
    fun `capacity guard refuses overload without a manager and allows with override`() = runTest {
        val heavy = insertBooked("IND/2627/90003", "Nashik", 9200) // 200 kg over the 9,000 kg vehicle

        val refused = repository.createTrip(
            CreateTripDraft(
                vehicleId = SeedIds.VEHICLE_MH15BK4412, driverId = SeedIds.DRIVER_GURMEET,
                originBranchId = branch, destStationId = "seed-station-1", viaStationIds = emptyList(),
                consignmentIds = listOf(heavy), hirePaise = 100_000, advancePaise = 0,
            ),
            now,
        )
        assertTrue(refused is Result.Failure)
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, (refused as Result.Failure).code)
        assertTrue((refused.message ?: "").contains("200 kg over"))

        val allowed = repository.createTrip(
            CreateTripDraft(
                vehicleId = SeedIds.VEHICLE_MH15BK4412, driverId = SeedIds.DRIVER_GURMEET,
                originBranchId = branch, destStationId = "seed-station-1", viaStationIds = emptyList(),
                consignmentIds = listOf(heavy), hirePaise = 100_000, advancePaise = 0,
                managerOverride = true,
            ),
            now,
        ).getOrNull()!!
        assertEquals("the overloaded trip exists with its single leg", 1, database.tripDao().countLegs(allowed.tripLocalId))
    }

    @Test
    fun `a vehicle holds at most one open trip`() = runTest {
        val a = insertBooked("IND/2627/90001", "Nashik", 500)
        val b = insertBooked("IND/2627/90002", "Dhule", 500)
        val draft: (String) -> CreateTripDraft = { consignment ->
            CreateTripDraft(
                vehicleId = SeedIds.VEHICLE_MH15BK4412, driverId = SeedIds.DRIVER_GURMEET,
                originBranchId = branch, destStationId = "seed-station-1", viaStationIds = emptyList(),
                consignmentIds = listOf(consignment), hirePaise = 100_000, advancePaise = 0,
            )
        }

        val first = repository.createTrip(draft(a), now).getOrNull()!!
        val second = repository.createTrip(draft(b), now)
        assertTrue("create is guarded too (T10's error banner)", second is Result.Failure)
        assertEquals(ErrorCode.TRIP_VEHICLE_BUSY, (second as Result.Failure).code)

        repository.issue(first.tripLocalId, now + 1)
        val third = repository.createTrip(draft(b), now)
        assertEquals("issued still counts as open", ErrorCode.TRIP_VEHICLE_BUSY, (third as Result.Failure).code)

        val thirdRetry = repository.createTrip(
            draft(b).copy(vehicleId = "seed-vehicle-mp09gh2207"),
            now,
        ).getOrNull()!!
        assertTrue("another vehicle works", thirdRetry.challanNo == null)

        repository.dispatch(first.tripLocalId, now + 2)
        repository.close(first.tripLocalId, now + 3)
        val after = repository.createTrip(draft(b).copy(vehicleId = SeedIds.VEHICLE_MH15BK4412), now + 4)
        assertTrue("closing frees the vehicle", after.getOrNull() != null)
    }

    @Test
    fun `dispatch then close split arrivals from onward-leg hubs`() = runTest {
        val nashik = insertBooked("IND/2627/90001", "Nashik", 700)
        val dhule = insertBooked("IND/2627/90002", "Dhule", 300)
        val trip = repository.createTrip(
            CreateTripDraft(
                vehicleId = SeedIds.VEHICLE_MH15BK4412, driverId = SeedIds.DRIVER_GURMEET,
                originBranchId = branch, destStationId = "seed-station-1", viaStationIds = emptyList(),
                consignmentIds = listOf(nashik, dhule), hirePaise = 100_000, advancePaise = 0,
            ),
            now,
        ).getOrNull()!!
        repository.issue(trip.tripLocalId, now + 1)
        repository.dispatch(trip.tripLocalId, now + 2)

        assertEquals("dispatch writes an InTransit event for the consignment", 1, database.consignmentDao().getEvents(nashik).count { it.event_type == "IN_TRANSIT" })

        repository.close(trip.tripLocalId, now + 3)

        val nashikEvents = database.consignmentDao().getEvents(nashik).map { it.event_type }
        val dhuleEvents = database.consignmentDao().getEvents(dhule).map { it.event_type }
        assertTrue("Nashik was the trip's destination: Arrived", "ARRIVED" in nashikEvents)
        assertTrue("Dhule was a via stop: At hub for the onward challan", "AT_HUB" in dhuleEvents)
        assertTrue("Dhule never arrived on this trip", "ARRIVED" !in dhuleEvents)

        assertEquals("CLOSED", database.tripDao().getTrip(trip.tripLocalId)!!.state)
    }

    @Test
    fun `board projection follows the trip lifecycle`() = runTest {
        val consignment = insertBooked("IND/2627/90001", "Nashik", 500)
        val vehicle = SeedIds.VEHICLE_MH15BK4412

        assertTrue("available before anything", !boardRow(vehicle).onTrip)

        val trip = repository.createTrip(
            CreateTripDraft(
                vehicleId = vehicle, driverId = SeedIds.DRIVER_GURMEET,
                originBranchId = branch, destStationId = "seed-station-1", viaStationIds = emptyList(),
                consignmentIds = listOf(consignment), hirePaise = 100_000, advancePaise = 0,
            ),
            now,
        ).getOrNull()!!
        assertTrue("a being-built trip is not yet on the board", !boardRow(vehicle).onTrip)

        val issued = repository.issue(trip.tripLocalId, now + 1).getOrNull()!!
        val row = boardRow(vehicle)
        assertTrue(row.onTrip)
        assertEquals("CHL/IND/2627/00742", row.challanNo)
        assertEquals("Gurmeet Singh", row.driverName)
        assertEquals(500L, row.loadKg)
        assertTrue(!row.isLate)

        repository.dispatch(trip.tripLocalId, now + 2)
        repository.close(trip.tripLocalId, now + 3)
        val closed = boardRow(vehicle)
        assertTrue("closed frees the vehicle", !closed.onTrip)
        assertEquals("closed today, so idle zero days", 0L, closed.idleDays)
        assertTrue(issued.challanNo != null)
    }

    @Test
    fun `addCost requires the remark`() = runTest {
        val consignment = insertBooked("IND/2627/90001", "Nashik", 500)
        val trip = repository.createTrip(
            CreateTripDraft(
                vehicleId = SeedIds.VEHICLE_MH15BK4412, driverId = SeedIds.DRIVER_GURMEET,
                originBranchId = branch, destStationId = "seed-station-1", viaStationIds = emptyList(),
                consignmentIds = listOf(consignment), hirePaise = 100_000, advancePaise = 0,
            ),
            now,
        ).getOrNull()!!

        val refused = repository.addCost(trip.tripLocalId, "toll", 5_000, "CASH", "  ", now)
        assertTrue(refused is Result.Failure)

        repository.addCost(trip.tripLocalId, "toll", 5_000, "CASH", "Bharuch toll plaza", now)
        assertEquals(5_000L, database.tripDao().sumCosts(trip.tripLocalId))
    }

    private suspend fun boardRow(vehicleId: String): BoardRow =
        repository.observeBoard(company).first().first { it.vehicleLocalId == vehicleId }
}
