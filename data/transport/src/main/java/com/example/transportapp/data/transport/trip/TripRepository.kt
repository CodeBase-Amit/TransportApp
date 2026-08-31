package com.example.transportapp.data.transport.trip

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.dao.TripDao
import com.example.transportapp.core.database.entity.LorryHireEntity
import com.example.transportapp.core.database.entity.TripCostEntity
import com.example.transportapp.core.database.entity.TripEntity
import com.example.transportapp.core.database.entity.TripLegEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.numbering.NumberingRepository
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.domain.transport.TripState
import com.example.transportapp.domain.transport.consignment.ConsignmentStateMachine
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.trip.TripStateMachine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One pickable consignment in the T10 loadable pool (§11.2). */
data class LoadableConsignment(
    val localId: String,
    val displayNo: String,
    val consigneeName: String,
    val toStationId: String,
    val toStation: String,
    val status: ConsignmentStatus,
    val paymentMode: String?,
    val packages: Long,
    val weightKg: Long,
    val totalPaise: Long,
)

data class VehicleOption(val localId: String, val number: String, val capacityKg: Int, val ownership: String, val ownershipLabel: String)

data class DriverOption(val localId: String, val name: String, val licence: String?, val phone: String?)

/** What the T10 builder hands over when the clerk taps "Create challan". */
data class CreateTripDraft(
    val vehicleId: String,
    val driverId: String,
    val originBranchId: String,
    val destStationId: String,
    val viaStationIds: List<String>,
    val consignmentIds: List<String>,
    val hirePaise: Long,
    val advancePaise: Long,
    /** §11.2: overload needs a Manager to proceed — the app warns, never hard-blocks. */
    val managerOverride: Boolean = false,
)

data class TripRef(val tripLocalId: String, val challanNo: String?)

/** T11's detail: the trip, its legs grouped by destination, vehicle/driver, hire. */
data class TripDetail(
    val tripLocalId: String,
    val challanNo: String?,
    val state: TripState,
    val originBranchName: String,
    val destStation: String,
    val viaStations: List<String>,
    val createdLine: String,
    val consignments: Int,
    val loadKg: Long,
    val hirePaise: Long,
    val advancePaise: Long,
    val balancePaise: Long,
    val vehicleNumber: String,
    val vehicleOwnership: String,
    val vehicleCapacityKg: Int,
    val driverName: String,
    val driverLicence: String?,
    val driverPhone: String?,
    val legs: List<TripLeg>,
    val dispatchedAt: Long?,
    val closedAt: Long?,
) {
    data class TripLeg(val displayNo: String, val consigneeName: String, val toStation: String, val paymentMode: String?, val weightKg: Long)
}

/** One row of the §11.4 vehicle board. */
data class BoardRow(
    val vehicleLocalId: String,
    val number: String,
    val capacityKg: Int,
    val ownership: String,
    val onTrip: Boolean,
    val tripLocalId: String?,
    val challanNo: String?,
    val tripState: TripState?,
    val destStation: String?,
    val isLate: Boolean,
    val driverName: String?,
    val loadKg: Long?,
    val consignments: Int?,
    val idleDays: Long?,
)

/**
 * Trips and challans (Phase2.md S7). Create/issue/dispatch/close all run in one Room
 * transaction; issue/dispatch/close write a status event per loaded consignment and one
 * outbox row per consignment (§7.2's bulk rule). Guards use §18.3 codes; a state-machine
 * violation is a programming error and throws.
 */
interface TripRepository {

    fun observeLoadablePool(companyId: String, branchId: String): Flow<List<LoadableConsignment>>

    suspend fun availableVehicles(companyId: String): List<VehicleOption>

    suspend fun drivers(companyId: String): List<DriverOption>

    suspend fun createTrip(draft: CreateTripDraft, now: Long): Result<TripRef>

    suspend fun issue(tripId: String, now: Long): Result<TripRef>

    suspend fun dispatch(tripId: String, now: Long): Result<Unit>

    suspend fun close(tripId: String, now: Long): Result<Unit>

    suspend fun cancel(tripId: String, reason: String, now: Long): Result<Unit>

    suspend fun tripDetail(challanNo: String): TripDetail?

    fun observeBoard(companyId: String): Flow<List<BoardRow>>

    suspend fun addCost(tripId: String, head: String, amountPaise: Long, paymentMode: String, remark: String, now: Long): Result<Unit>
}

@Singleton
class TripRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val tripDao: TripDao,
    private val sessionRepository: com.example.transportapp.data.transport.session.SessionRepository,
    private val numberingRepository: NumberingRepository,
    private val outboxWriter: OutboxWriter,
) : TripRepository {

    /** The session read once per call — the demo session is stable for the app's lifetime. */
    private suspend fun session(): com.example.transportapp.data.transport.session.UserSession =
        sessionRepository.session.first()

    override fun observeLoadablePool(companyId: String, branchId: String): Flow<List<LoadableConsignment>> =
        tripDao.observeLoadablePool(companyId, branchId).map { rows ->
            rows.map { row ->
                LoadableConsignment(
                    localId = row.local_id,
                    displayNo = row.display_no,
                    consigneeName = row.consignee_name,
                    toStationId = row.to_station_id,
                    toStation = row.to_station,
                    status = runCatching { ConsignmentStatus.valueOf(row.status) }.getOrDefault(ConsignmentStatus.BOOKED),
                    paymentMode = row.payment_mode,
                    packages = row.packages,
                    weightKg = row.weight_kg,
                    totalPaise = row.total_paise,
                )
            }
        }

    override suspend fun availableVehicles(companyId: String): List<VehicleOption> =
        tripDao.getAvailableVehicles(companyId).map { v ->
            VehicleOption(
                localId = v.local_id,
                number = v.number,
                capacityKg = v.capacity_kg,
                ownership = v.ownership,
                ownershipLabel = when (v.ownership) {
                    "OWN" -> "Own"
                    "ATTACHED" -> "Attached"
                    else -> "Market"
                },
            )
        }

    override suspend fun drivers(companyId: String): List<DriverOption> =
        tripDao.getDrivers(companyId).map { d -> DriverOption(d.local_id, d.name, d.licence, d.phone) }

    override suspend fun createTrip(draft: CreateTripDraft, now: Long): Result<TripRef> {
        if (draft.consignmentIds.isEmpty()) {
            return Result.failure(ErrorCode.CAPACITY_EXCEEDED, "Pick at least one consignment to load")
        }
        return database.withTransaction {
            val session = session()
                ?: return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
            val vehicle = tripDao.getVehicle(draft.vehicleId)
                ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Vehicle not found")

            tripDao.getAssignedTripForVehicle(session.companyId, draft.vehicleId)?.let { open ->
                return@withTransaction Result.failure(
                    ErrorCode.TRIP_VEHICLE_BUSY,
                    "${vehicle.number} is already on a challan${open.challan_no?.let { " ($it)" } ?: ""}. Close that trip first, or pick another vehicle.",
                )
            }

            val legs = draft.consignmentIds.mapNotNull { consignmentDao().getConsignment(it) }
            if (legs.size != draft.consignmentIds.size) {
                return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "A selected consignment no longer exists")
            }
            val loadG = legs.sumOf { it.chargeable_weight_g }
            val capacityG = vehicle.capacity_kg * 1000L
            if (loadG > capacityG && !draft.managerOverride) {
                val overKg = (loadG - capacityG) / 1000
                return@withTransaction Result.failure(
                    ErrorCode.CAPACITY_EXCEEDED,
                    "$overKg kg over capacity. A manager has to approve this challan before dispatch.",
                )
            }

            val tripId = "trip-" + UUID.randomUUID().toString()
            tripDao.upsertTrip(
                TripEntity(
                    local_id = tripId, server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null, company_id = session.companyId,
                    series_id = "", challan_no = null, state = TripState.OPEN.name,
                    vehicle_id = draft.vehicleId, driver_id = draft.driverId,
                    origin_branch_id = draft.originBranchId, dest_station_id = draft.destStationId,
                    via_stations = draft.viaStationIds.joinToString(","),
                    hire_paise = draft.hirePaise, advance_paise = draft.advancePaise,
                    balance_paise = draft.hirePaise - draft.advancePaise,
                    expected_arrival = now, created_at = now, created_by_name = session.name,
                    dispatched_at = null, closed_at = null, cancel_reason = null,
                ),
            )
            draft.consignmentIds.forEachIndexed { index, consignmentId ->
                tripDao.upsertLeg(
                    TripLegEntity(
                        local_id = "tl-" + UUID.randomUUID().toString(), server_id = null,
                        updated_at_local = now, updated_at_server = null, sync_state = SyncState.PENDING, deleted_at = null,
                        trip_id = tripId, consignment_id = consignmentId, leg_no = index + 1, loaded_at = now,
                    ),
                )
            }
            tripDao.upsertLorryHire(
                LorryHireEntity(
                    local_id = "lh-" + UUID.randomUUID().toString(), server_id = null,
                    updated_at_local = now, updated_at_server = null, sync_state = SyncState.PENDING, deleted_at = null,
                    trip_id = tripId, owner_party_id = null, broker_id = null,
                    hire_paise = draft.hirePaise, advance_paise = draft.advancePaise,
                    deductions_paise = 0, balance_paise = draft.hirePaise - draft.advancePaise,
                ),
            )
            Result.success(TripRef(tripId, challanNo = null))
        }
    }

    override suspend fun issue(tripId: String, now: Long): Result<TripRef> = database.withTransaction {
        val trip = tripDao.getTrip(tripId) ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Trip not found")
        requireState(trip, TripState.OPEN)
        val session = session() ?: return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")

        // §11.1: at issue time the vehicle must still hold no open trip.
        tripDao.getOpenTripForVehicle(trip.company_id, trip.vehicle_id)?.let { open ->
            if (open.local_id != tripId) {
                return@withTransaction Result.failure(ErrorCode.TRIP_VEHICLE_BUSY, "The vehicle is already on an open trip")
            }
        }

        val number = numberingRepository.issueNext(trip.company_id, trip.origin_branch_id, "CHALLAN", now)
        val issued = (number as? com.example.transportapp.core.common.Result.Success)?.value
            ?: return@withTransaction Result.failure(
                (number as com.example.transportapp.core.common.Result.Failure).code,
                (number as com.example.transportapp.core.common.Result.Failure).message,
            )
        require(!issued.provisional) { "challan numbering never falls through in this demo" }

        val legs = tripDao.getLegRows(tripId)
        val challanSeries = database.numberingDao().getSeries(trip.company_id, trip.origin_branch_id, "CHALLAN")
        tripDao.upsertTrip(
            trip.copy(
                series_id = challanSeries?.local_id ?: trip.series_id,
                challan_no = issued.display,
                state = TripState.ISSUED.name,
                updated_at_local = now,
            ),
        )
        legs.forEach { leg ->
            advanceConsignment(leg.consignment_id, trip.company_id, ConsignmentStatus.LOADED, "LOADED", session.name, trip.origin_branch_id, now)
        }
        Result.success(TripRef(tripId, issued.display))
    }

    override suspend fun dispatch(tripId: String, now: Long): Result<Unit> = database.withTransaction {
        val trip = tripDao.getTrip(tripId) ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Trip not found")
        requireState(trip, TripState.ISSUED)
        val session = session() ?: return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")

        val legs = tripDao.getLegRows(tripId)
        val expected = now + 2L * 24 * 60 * 60 * 1000
        tripDao.upsertTrip(trip.copy(state = TripState.DISPATCHED.name, dispatched_at = now, expected_arrival = expected, updated_at_local = now))
        legs.forEach { leg ->
            advanceConsignment(leg.consignment_id, trip.company_id, ConsignmentStatus.IN_TRANSIT, "IN_TRANSIT", session.name, trip.origin_branch_id, now)
        }
        Result.success(Unit)
    }

    override suspend fun close(tripId: String, now: Long): Result<Unit> = database.withTransaction {
        val trip = tripDao.getTrip(tripId) ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Trip not found")
        requireState(trip, TripState.DISPATCHED)
        val session = session() ?: return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")

        val legs = tripDao.getLegRows(tripId)
        val destStation = database.mastersDao().getStation(trip.dest_station_id)
        // §11.2 close rule: consignments whose destination this is get Arrived; the rest are
        // at a hub for their onward challan.
        legs.forEach { leg ->
            val arrived = leg.to_station == destStation?.name
            val target = if (arrived) ConsignmentStatus.ARRIVED else ConsignmentStatus.AT_HUB
            advanceConsignment(leg.consignment_id, trip.company_id, target, if (arrived) "ARRIVED" else "AT_HUB", session.name, trip.origin_branch_id, now)
        }
        tripDao.upsertTrip(trip.copy(state = TripState.CLOSED.name, closed_at = now, updated_at_local = now))
        Result.success(Unit)
    }

    override suspend fun cancel(tripId: String, reason: String, now: Long): Result<Unit> = database.withTransaction {
        val trip = tripDao.getTrip(tripId) ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Trip not found")
        requireState(trip, TripState.OPEN, TripState.ISSUED)
        if (reason.length < 10) {
            return@withTransaction Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "A cancellation needs a reason of at least ten characters")
        }
        tripDao.upsertTrip(trip.copy(state = TripState.CANCELLED.name, cancel_reason = reason, updated_at_local = now))
        // §11.1: consignments return to the pool — nothing to write, the legs simply no
        // longer count against a live trip.
        Result.success(Unit)
    }

    override suspend fun tripDetail(challanNo: String): TripDetail? {
        val session = session() ?: return null
        val trip = tripDao.getTripByChallanNo(session.companyId, challanNo) ?: return null
        val vehicle = tripDao.getVehicle(trip.vehicle_id)
        val driver = tripDao.getDriver(trip.driver_id)
        val originBranch = database.orgDao().getBranchesForCompany(trip.company_id).firstOrNull { it.local_id == trip.origin_branch_id }
        val destStation = database.mastersDao().getStation(trip.dest_station_id)?.name ?: ""
        val via = trip.via_stations?.split(',')?.mapNotNull { database.mastersDao().getStation(it)?.name } ?: emptyList()
        val legs = tripDao.getLegRows(tripId = trip.local_id)
        val loadKg = legs.sumOf { it.weight_kg }
        return TripDetail(
            tripLocalId = trip.local_id,
            challanNo = trip.challan_no,
            state = runCatching { TripState.valueOf(trip.state) }.getOrDefault(TripState.OPEN),
            originBranchName = originBranch?.name ?: "",
            destStation = destStation,
            viaStations = via,
            createdLine = "created ${formatDateTime(trip.created_at)} by ${trip.created_by_name}",
            consignments = legs.size,
            loadKg = loadKg,
            hirePaise = trip.hire_paise,
            advancePaise = trip.advance_paise,
            balancePaise = trip.balance_paise,
            vehicleNumber = vehicle?.number ?: "",
            vehicleOwnership = vehicle?.ownership ?: "",
            vehicleCapacityKg = vehicle?.capacity_kg ?: 0,
            driverName = driver?.name ?: "",
            driverLicence = driver?.licence,
            driverPhone = driver?.phone,
            legs = legs.map { TripDetail.TripLeg(it.display_no, it.consignee_name, it.to_station, it.payment_mode, it.weight_kg) },
            dispatchedAt = trip.dispatched_at,
            closedAt = trip.closed_at,
        )
    }

    override fun observeBoard(companyId: String): Flow<List<BoardRow>> =
        tripDao.observeBoard(companyId, System.currentTimeMillis()).map { rows ->
            rows.map { row ->
                val state = row.trip_state?.let { runCatching { TripState.valueOf(it) }.getOrNull() }
                val expectedArrival: Long? = row.expected_arrival
                BoardRow(
                    vehicleLocalId = row.local_id,
                    number = row.reg_no,
                    capacityKg = (row.capacity_g / 1000).toInt(),
                    ownership = row.ownership,
                    onTrip = row.vehicle_state == "ON_TRIP",
                    tripLocalId = row.trip_local_id,
                    challanNo = row.challan_no,
                    tripState = state,
                    destStation = row.dest_station,
                    isLate = state == TripState.DISPATCHED && expectedArrival != null && System.currentTimeMillis() > expectedArrival,
                    driverName = row.driver_name,
                    loadKg = row.load_kg,
                    consignments = row.consignments,
                    idleDays = row.idle_days,
                )
            }
        }

    override suspend fun addCost(tripId: String, head: String, amountPaise: Long, paymentMode: String, remark: String, now: Long): Result<Unit> {
        if (remark.isBlank()) {
            return Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "A trip cost needs a remark")
        }
        val trip = tripDao.getTrip(tripId) ?: return Result.failure(ErrorCode.MASTER_IN_USE, "Trip not found")
        tripDao.upsertCost(
            TripCostEntity(
                local_id = "tc-" + UUID.randomUUID().toString(), server_id = null,
                updated_at_local = now, updated_at_server = null, sync_state = SyncState.PENDING, deleted_at = null,
                trip_id = tripId, vehicle_id = trip.vehicle_id, head = head,
                incurred_on = now, amount_paise = amountPaise, payment_mode = paymentMode, remark = remark,
            ),
        )
        return Result.success(Unit)
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private fun requireState(trip: TripEntity, vararg legal: TripState) {
        val state = runCatching { TripState.valueOf(trip.state) }.getOrDefault(TripState.OPEN)
        require(trip.state in legal.map { it.name }) { "trip is ${trip.state}, expected ${legal.joinToString()}" }
    }

    /**
     * One consignment, one transition: the §7.1 state machine is checked against the
     * current projection, the event is appended, and the projection moves with it (§3.4
     * #3 — the projection is written only by repository code, D1).
     */
    private suspend fun advanceConsignment(
        consignmentId: String,
        companyId: String,
        target: ConsignmentStatus,
        eventType: String,
        actorName: String,
        branchId: String,
        now: Long,
    ) {
        val consignment = database.consignmentDao().getConsignment(consignmentId) ?: return
        val from = runCatching { ConsignmentStatus.valueOf(consignment.status_projection) }.getOrDefault(ConsignmentStatus.BOOKED)
        if (!ConsignmentStateMachine.canTransition(from, target)) return
        val clientEventId = UUID.randomUUID().toString()
        database.consignmentDao().insertStatusEvent(
            com.example.transportapp.core.database.entity.StatusEventEntity(
                local_id = "ev-" + UUID.randomUUID().toString(), server_id = null,
                updated_at_local = now, updated_at_server = null, sync_state = SyncState.PENDING, deleted_at = null,
                company_id = companyId, consignment_id = consignmentId, client_event_id = clientEventId,
                event_type = eventType, occurred_at = now, recorded_at = now,
                actor_member_id = null, actor_name = actorName, branch_id = branchId,
                location = null, photo_ref = null, reason_code = null, remark = null, challan_ref = null,
            ),
        )
        database.consignmentDao().upsertConsignment(consignment.copy(status_projection = target.name, updated_at_local = now))
        outboxWriter.enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.STATUS_EVENT,
            entityLocalId = consignmentId,
            payloadJson = org.json.JSONObject()
                .put("consignment_id", consignmentId)
                .put("client_event_id", clientEventId)
                .put("event_type", eventType)
                .put("occurred_at", now)
                .put("actor_name", actorName)
                .put("branch_id", branchId)
                .toString(),
            now = now,
        )
    }

    private fun formatDateTime(epoch: Long): String =
        java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.ENGLISH).format(java.util.Date(epoch))

    private fun consignmentDao() = database.consignmentDao()
}
