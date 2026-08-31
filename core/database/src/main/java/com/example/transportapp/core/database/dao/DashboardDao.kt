package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query

/** §13 "Consignments in transit" — count and packages on the road. */
data class InTransitRow(val cnt: Int, val packages: Long)

/** §13 "Booked today" — count, chargeable weight, freight. */
data class BookedTodayRow(val cnt: Int, val weightG: Long, val freightPaise: Long)

/** §13 "To Pay to collect" — count and amount awaiting collection. */
data class TopayAwaitingRow(val cnt: Int, val amountPaise: Long)

/** §13 "Unbilled freight" — amount not yet on a bill and the oldest booking. */
data class UnbilledRow(val amountPaise: Long, val oldestBookedAt: Long?)

/** §13 "Receivable outstanding" — issued bills minus allocated receipts, with the 90+ slice. */
data class ReceivableRow(val outstandingPaise: Long, val over90Paise: Long)

/** §13 "This month" — freight earned, lorry hire paid, trip costs, for margin. */
data class MonthMoneyRow(val freightPaise: Long, val hirePaise: Long, val costPaise: Long)

/** §13 "Undelivered ageing" — consignments past expected arrival + grace, bucketed by days late. */
data class AgeingBucketsRow(val bucket1to3: Int, val bucket4to7: Int, val bucket7plus: Int)

/** One idle own vehicle: no open trip and idle for more than the threshold. */
data class IdleVehicleRow(val vehicle_id: String, val number: String, val idle_days: Long)

/** Held exceptions in the window, grouped by reason (§13 exceptions tile). */
data class ExceptionReasonRow(val reason_code: String?, val cnt: Int)

/**
 * The §13 dashboard tiles as read-only aggregates. One query per tile — the repository runs
 * them in parallel and stamps "as of" from its own clock (§13: never pretend to be live).
 */
@Dao
interface DashboardDao {

    @Query("SELECT COUNT(*) FROM TRIP_E WHERE company_id = :companyId AND state = 'DISPATCHED' AND deleted_at IS NULL")
    suspend fun countRunningTrips(companyId: String): Int

    @Query("SELECT MIN(expected_arrival) FROM TRIP_E WHERE company_id = :companyId AND state = 'DISPATCHED' AND deleted_at IS NULL")
    suspend fun nearestRunningArrival(companyId: String): Long?

    @Query(
        """
        SELECT COUNT(*) AS cnt, COALESCE(SUM(packages), 0) AS packages
        FROM CONSIGNMENT_E WHERE company_id = :companyId AND status_projection = 'IN_TRANSIT' AND deleted_at IS NULL
        """,
    )
    suspend fun inTransit(companyId: String): InTransitRow

    @Query(
        """
        SELECT COUNT(*) AS cnt,
               COALESCE(SUM(chargeable_weight_g), 0) AS weightG,
               COALESCE(SUM(freight_paise), 0) AS freightPaise
        FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND booked_at >= :startOfDay
          AND (:branchId IS NULL OR booking_branch_id = :branchId)
        """,
    )
    suspend fun bookedToday(companyId: String, branchId: String?, startOfDay: Long): BookedTodayRow

    @Query(
        """
        SELECT COUNT(*) AS cnt, COALESCE(SUM(total_paise), 0) AS amountPaise
        FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND payment_mode = 'TOPAY'
          AND status_projection IN ('ARRIVED', 'OUT_FOR_DELIVERY')
          AND (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
               WHERE A.consignment_id = CONSIGNMENT_E.local_id AND A.target_type = 'TOPAY_CONSIGNMENT' AND A.deleted_at IS NULL) = 0
          AND (:branchId IS NULL OR booking_branch_id = :branchId)
        """,
    )
    suspend fun topayAwaiting(companyId: String, branchId: String?): TopayAwaitingRow

    @Query(
        """
        SELECT COALESCE(SUM(total_paise), 0) AS amountPaise, MIN(booked_at) AS oldestBookedAt
        FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND payment_mode = 'TBB' AND freight_bill_id IS NULL
          AND status_projection NOT IN ('CANCELLED')
        """,
    )
    suspend fun unbilled(companyId: String): UnbilledRow

    @Query(
        """
        SELECT COALESCE(SUM(B.total_paise - (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
                WHERE A.bill_id = B.local_id AND A.target_type = 'BILL' AND A.deleted_at IS NULL)), 0) AS outstandingPaise,
               COALESCE(SUM(CASE WHEN B.issued_at IS NOT NULL AND :now - B.issued_at > 90 * 86400000
                    THEN B.total_paise - (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
                         WHERE A.bill_id = B.local_id AND A.target_type = 'BILL' AND A.deleted_at IS NULL)
                    ELSE 0 END), 0) AS over90Paise
        FROM FREIGHT_BILL_E B
        WHERE B.company_id = :companyId AND B.state = 'ISSUED' AND B.deleted_at IS NULL
        """,
    )
    suspend fun receivable(companyId: String, now: Long): ReceivableRow

    @Query(
        """
        SELECT reason_code, COUNT(*) AS cnt
        FROM STATUS_EVENT_E E
        JOIN CONSIGNMENT_E C ON C.local_id = E.consignment_id
        WHERE E.company_id = :companyId AND E.deleted_at IS NULL
          AND E.event_type IN ('HELD', 'RETURNED') AND E.occurred_at >= :sinceAt
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
        GROUP BY reason_code
        ORDER BY cnt DESC
        """,
    )
    suspend fun exceptionReasons(companyId: String, branchId: String?, sinceAt: Long): List<ExceptionReasonRow>

    @Query(
        """
        SELECT
            SUM(CASE WHEN :now - expected_arrival <= 3 * 86400000 THEN 1 ELSE 0 END) AS bucket1to3,
            SUM(CASE WHEN :now - expected_arrival > 3 * 86400000 AND :now - expected_arrival <= 7 * 86400000 THEN 1 ELSE 0 END) AS bucket4to7,
            SUM(CASE WHEN :now - expected_arrival > 7 * 86400000 THEN 1 ELSE 0 END) AS bucket7plus
        FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND status_projection NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')
          AND expected_arrival < :overdueBefore
          AND (:branchId IS NULL OR booking_branch_id = :branchId)
        """,
    )
    suspend fun overdueBuckets(companyId: String, branchId: String?, now: Long, overdueBefore: Long): AgeingBucketsRow

    @Query(
        """
        SELECT V.local_id AS vehicle_id, V.number AS number,
               CAST((:now - COALESCE(MAX(COALESCE(T.closed_at, T.created_at)), :neverDrivenEpoch)) / 86400000 AS INTEGER) AS idle_days
        FROM VEHICLE_E V
        LEFT JOIN TRIP_E T ON T.vehicle_id = V.local_id AND T.deleted_at IS NULL
        WHERE V.company_id = :companyId AND V.ownership = 'OWN' AND V.deleted_at IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM TRIP_E O WHERE O.vehicle_id = V.local_id
                AND O.state IN ('OPEN', 'ISSUED', 'DISPATCHED') AND O.deleted_at IS NULL
          )
        GROUP BY V.local_id
        HAVING idle_days >= :idleDays
        ORDER BY idle_days DESC
        """,
    )
    suspend fun idleVehicles(companyId: String, now: Long, idleDays: Long, neverDrivenEpoch: Long): List<IdleVehicleRow>

    @Query(
        """
        SELECT COALESCE(SUM(freight_paise), 0) AS freightPaise,
               (SELECT COALESCE(SUM(T.hire_paise), 0) FROM TRIP_E T
                WHERE T.company_id = :companyId AND T.deleted_at IS NULL AND T.created_at >= :startOfMonth AND T.created_at < :endOfMonth) AS hirePaise,
               (SELECT COALESCE(SUM(C.amount_paise), 0) FROM TRIP_COST_E C
                JOIN TRIP_E T ON T.local_id = C.trip_id
                WHERE T.company_id = :companyId AND T.deleted_at IS NULL AND C.incurred_on >= :startOfMonth AND C.incurred_on < :endOfMonth) AS costPaise
        FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND status_projection NOT IN ('CANCELLED')
          AND booked_at >= :startOfMonth AND booked_at < :endOfMonth
        """,
    )
    suspend fun monthMoney(companyId: String, startOfMonth: Long, endOfMonth: Long): MonthMoneyRow
}
