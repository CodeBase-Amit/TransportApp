package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.LorryHireEntity
import com.example.transportapp.core.database.entity.TripCostEntity
import com.example.transportapp.core.database.entity.TripEntity
import com.example.transportapp.core.database.entity.TripLegEntity
import kotlinx.coroutines.flow.Flow

/** One consignment in the T10 loadable pool (§11.2). */
data class LoadableConsignmentRow(
    val local_id: String,
    val display_no: String,
    val consignee_name: String,
    val to_station_id: String,
    val to_station: String,
    val status: String,
    val payment_mode: String,
    val packages: Long,
    val weight_kg: Long,
    val total_paise: Long,
    val on_open_trip: Boolean,
)

/** One row of the §11.4 vehicle board. */
data class VehicleBoardRow(
    val local_id: String,
    val reg_no: String,
    val capacity_g: Long,
    val ownership: String,
    val vehicle_state: String,
    val trip_local_id: String?,
    val challan_no: String?,
    val trip_state: String?,
    val dest_station: String?,
    val expected_arrival: Long?,
    val driver_name: String?,
    val load_kg: Long?,
    val consignments: Int?,
    val idle_days: Long?,
)

/** One leg row of the T11 "what's loaded" list. */
data class TripLegRow(
    val consignment_id: String,
    val display_no: String,
    val consignee_name: String,
    val to_station: String,
    val payment_mode: String,
    val weight_kg: Long,
)

@Dao
interface TripDao {

    @Upsert
    suspend fun upsertTrip(entity: TripEntity)

    @Query("SELECT * FROM TRIP_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getTrip(localId: String): TripEntity?

    @Query("SELECT * FROM TRIP_E WHERE company_id = :companyId AND challan_no = :challanNo AND deleted_at IS NULL")
    suspend fun getTripByChallanNo(companyId: String, challanNo: String): TripEntity?

    /** §11.1: at most one OPEN (Issued/Dispatched) trip per vehicle. */
    @Query(
        """
        SELECT * FROM TRIP_E
        WHERE vehicle_id = :vehicleId AND company_id = :companyId AND deleted_at IS NULL
          AND state IN ('ISSUED', 'DISPATCHED')
        LIMIT 1
        """,
    )
    suspend fun getOpenTripForVehicle(companyId: String, vehicleId: String): TripEntity?

    /**
     * The create-time assignment guard is stricter than §11.1's open family: a truck
     * assigned to a challan that is still being built is de facto committed, and letting a
     * second builder proceed would only move the collision to issue time.
     */
    @Query(
        """
        SELECT * FROM TRIP_E
        WHERE vehicle_id = :vehicleId AND company_id = :companyId AND deleted_at IS NULL
          AND state IN ('OPEN', 'ISSUED', 'DISPATCHED')
        LIMIT 1
        """,
    )
    suspend fun getAssignedTripForVehicle(companyId: String, vehicleId: String): TripEntity?

    @Query(
        """
        SELECT * FROM TRIP_E
        WHERE company_id = :companyId AND deleted_at IS NULL AND state IN ('OPEN', 'ISSUED', 'DISPATCHED')
        """,
    )
    suspend fun getLiveTrips(companyId: String): List<TripEntity>

    @Query("SELECT COUNT(*) FROM TRIP_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countTrips(companyId: String): Int

    @Upsert
    suspend fun upsertLeg(entity: TripLegEntity)

    @Query("SELECT COUNT(*) FROM TRIP_LEG_E WHERE trip_id = :tripId AND deleted_at IS NULL")
    suspend fun countLegs(tripId: String): Int

    @Query(
        """
        SELECT
            C.local_id AS consignment_id,
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            P.name AS consignee_name,
            ST.name AS to_station,
            C.payment_mode AS payment_mode,
            C.chargeable_weight_g / 1000 AS weight_kg
        FROM TRIP_LEG_E L
        JOIN CONSIGNMENT_E C ON C.local_id = L.consignment_id
        JOIN PARTY_E P ON P.local_id = C.consignee_id
        JOIN STATION_E ST ON ST.local_id = C.to_station_id
        WHERE L.trip_id = :tripId AND L.deleted_at IS NULL
        ORDER BY ST.name, L.leg_no
        """,
    )
    suspend fun getLegRows(tripId: String): List<TripLegRow>

    /** Consignments already on a live leg of any trip — the pool must exclude them. */
    @Query(
        """
        SELECT L.consignment_id FROM TRIP_LEG_E L
        JOIN TRIP_E T ON T.local_id = L.trip_id
        WHERE T.company_id = :companyId AND T.deleted_at IS NULL AND L.deleted_at IS NULL
          AND T.state IN ('OPEN', 'ISSUED', 'DISPATCHED')
        """,
    )
    suspend fun getConsignmentIdsOnLiveTrips(companyId: String): List<String>

    /**
     * §11.2 loadable pool: Booked here, or At hub here (last AtHub event recorded at this
     * branch), and not already sitting on a live trip.
     */
    @Query(
        """
        SELECT
            C.local_id AS local_id,
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            P.name AS consignee_name,
            C.to_station_id AS to_station_id,
            ST.name AS to_station,
            C.status_projection AS status,
            C.payment_mode AS payment_mode,
            C.packages AS packages,
            C.chargeable_weight_g / 1000 AS weight_kg,
            C.total_paise AS total_paise,
            EXISTS (
                SELECT 1 FROM TRIP_LEG_E L JOIN TRIP_E T ON T.local_id = L.trip_id
                WHERE L.consignment_id = C.local_id AND T.deleted_at IS NULL
                  AND T.state IN ('OPEN', 'ISSUED', 'DISPATCHED')
            ) AS on_open_trip
        FROM CONSIGNMENT_E C
        JOIN PARTY_E P ON P.local_id = C.consignee_id
        JOIN STATION_E ST ON ST.local_id = C.to_station_id
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL
          AND (
              (C.status_projection = 'BOOKED' AND C.booking_branch_id = :branchId)
              OR (
                  C.status_projection = 'AT_HUB'
                  AND EXISTS (
                      SELECT 1 FROM STATUS_EVENT_E E
                      WHERE E.consignment_id = C.local_id AND E.event_type = 'AT_HUB'
                        AND E.branch_id = :branchId AND E.deleted_at IS NULL
                  )
              )
          )
          AND NOT EXISTS (
              SELECT 1 FROM TRIP_LEG_E L JOIN TRIP_E T ON T.local_id = L.trip_id
              WHERE L.consignment_id = C.local_id AND T.deleted_at IS NULL
                AND T.state IN ('OPEN', 'ISSUED', 'DISPATCHED')
          )
        ORDER BY C.booked_at DESC
        """,
    )
    fun observeLoadablePool(companyId: String, branchId: String): Flow<List<LoadableConsignmentRow>>

    @Upsert
    suspend fun upsertLorryHire(entity: LorryHireEntity)

    @Query("SELECT * FROM LORRY_HIRE_E WHERE trip_id = :tripId AND deleted_at IS NULL")
    suspend fun getLorryHire(tripId: String): LorryHireEntity?

    @Upsert
    suspend fun upsertCost(entity: TripCostEntity)

    @Query("SELECT COALESCE(SUM(amount_paise), 0) FROM TRIP_COST_E WHERE trip_id = :tripId AND deleted_at IS NULL")
    suspend fun sumCosts(tripId: String): Long

    /** §11.4 board: every vehicle with its open trip, load and idle days. */
    @Query(
        """
        SELECT
            V.local_id AS local_id,
            V.number AS reg_no,
            V.capacity_kg * 1000 AS capacity_g,
            V.ownership AS ownership,
            CASE WHEN T.state IN ('ISSUED', 'DISPATCHED') THEN 'ON_TRIP' ELSE 'AVAILABLE' END AS vehicle_state,
            T.local_id AS trip_local_id,
            T.challan_no AS challan_no,
            T.state AS trip_state,
            DST.name AS dest_station,
            T.expected_arrival AS expected_arrival,
            D.name AS driver_name,
            (
                SELECT COALESCE(SUM(C.chargeable_weight_g), 0) / 1000 FROM TRIP_LEG_E L
                JOIN CONSIGNMENT_E C ON C.local_id = L.consignment_id
                WHERE L.trip_id = T.local_id AND L.deleted_at IS NULL
            ) AS load_kg,
            (
                SELECT COUNT(*) FROM TRIP_LEG_E L
                WHERE L.trip_id = T.local_id AND L.deleted_at IS NULL
            ) AS consignments,
            (
                SELECT CAST((:now - MAX(T2.closed_at)) / 86400000 AS INTEGER) FROM TRIP_E T2
                WHERE T2.vehicle_id = V.local_id AND T2.state = 'CLOSED' AND T2.deleted_at IS NULL
            ) AS idle_days
        FROM VEHICLE_E V
        LEFT JOIN TRIP_E T
            ON T.vehicle_id = V.local_id AND T.deleted_at IS NULL AND T.state IN ('ISSUED', 'DISPATCHED')
        LEFT JOIN STATION_E DST ON DST.local_id = T.dest_station_id
        LEFT JOIN DRIVER_E D ON D.local_id = T.driver_id
        WHERE V.company_id = :companyId AND V.deleted_at IS NULL
        ORDER BY
            CASE WHEN T.state IN ('ISSUED', 'DISPATCHED') THEN 0 ELSE 1 END,
            V.number
        """,
    )
    fun observeBoard(companyId: String, now: Long): Flow<List<VehicleBoardRow>>

    @Query("SELECT * FROM VEHICLE_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getVehicle(localId: String): com.example.transportapp.core.database.entity.VehicleEntity?

    @Query("SELECT * FROM DRIVER_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getDriver(localId: String): com.example.transportapp.core.database.entity.DriverEntity?

    @Query(
        """
        SELECT * FROM VEHICLE_E WHERE company_id = :companyId AND deleted_at IS NULL AND local_id NOT IN (
            SELECT vehicle_id FROM TRIP_E WHERE company_id = :companyId AND deleted_at IS NULL AND state IN ('ISSUED', 'DISPATCHED')
        ) ORDER BY number
        """,
    )
    suspend fun getAvailableVehicles(companyId: String): List<com.example.transportapp.core.database.entity.VehicleEntity>

    @Query("SELECT * FROM DRIVER_E WHERE company_id = :companyId AND deleted_at IS NULL ORDER BY name")
    suspend fun getDrivers(companyId: String): List<com.example.transportapp.core.database.entity.DriverEntity>
}
