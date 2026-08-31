package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query

/** One freight-register row (T22 / §14 CSV). */
data class FreightRegisterDbRow(
    val display_no: String,
    val booked_at: Long,
    val branch: String,
    val consignor: String,
    val consignee: String,
    val route: String,
    val packages: Long,
    val weight_kg: Long,
    val freight_paise: Long,
    val gst_paise: Long,
    val total_paise: Long,
    val cancelled: Boolean,
)

data class RouteRevenueRow(val route: String, val freightPaise: Long, val cnt: Int)

data class PartyOutstandingRow(val party: String, val outstandingPaise: Long, val over90Paise: Long)

data class RegisterTotalsRow(val cnt: Int, val freightPaise: Long)

/** Row counts for the export centre's twelve checkbox rows (§B23). */
data class SheetCountsRow(
    val register: Long,
    val chargeLines: Long,
    val bills: Long,
    val receipts: Long,
    val allocations: Long,
    val topayCollections: Long,
    val trips: Long,
    val parties: Long,
)

/**
 * The report queries behind T21/T22 and the CSV pack (§14). Figures are projections over
 * synced data — no network on the critical path (§13).
 */
@Dao
interface ReportsDao {

    @Query(
        """
        SELECT
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            C.booked_at AS booked_at,
            BR.name AS branch,
            CO.name AS consignor,
            CN.name AS consignee,
            OS.name || ' → ' || DS.name AS route,
            C.packages AS packages,
            C.chargeable_weight_g / 1000 AS weight_kg,
            C.freight_paise AS freight_paise,
            C.gst_paise AS gst_paise,
            C.total_paise AS total_paise,
            C.status_projection = 'CANCELLED' AS cancelled
        FROM CONSIGNMENT_E C
        JOIN BRANCH_E BR ON BR.local_id = C.booking_branch_id
        JOIN PARTY_E CO ON CO.local_id = C.consignor_id
        JOIN PARTY_E CN ON CN.local_id = C.consignee_id
        JOIN STATION_E OS ON OS.local_id = C.from_station_id
        JOIN STATION_E DS ON DS.local_id = C.to_station_id
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL
          AND C.status_projection != 'DRAFT'
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
          AND C.booked_at >= :from AND C.booked_at <= :to
        ORDER BY C.booked_at DESC
        """,
    )
    suspend fun freightRegister(companyId: String, branchId: String?, from: Long, to: Long): List<FreightRegisterDbRow>

    @Query(
        """
        SELECT COUNT(*) AS cnt, COALESCE(SUM(freight_paise), 0) AS freightPaise FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND status_projection NOT IN ('DRAFT', 'CANCELLED') AND booked_at >= :from AND booked_at <= :to
        """,
    )
    suspend fun registerTotals(companyId: String, from: Long, to: Long): RegisterTotalsRow

    @Query(
        """
        SELECT COALESCE(SUM(B.total_paise - (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
             WHERE A.bill_id = B.local_id AND A.target_type = 'BILL' AND A.deleted_at IS NULL)), 0)
        FROM FREIGHT_BILL_E B
        WHERE B.company_id = :companyId AND B.state = 'ISSUED' AND B.deleted_at IS NULL
          AND (B.issued_at IS NULL OR B.issued_at <= :now)
        """,
    )
    suspend fun outstandingTotal(companyId: String, now: Long): Long

    @Query(
        """
        SELECT COALESCE(SUM(C.total_paise), 0) FROM CONSIGNMENT_E C
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL AND C.payment_mode = 'TOPAY'
          AND C.status_projection IN ('ARRIVED', 'OUT_FOR_DELIVERY')
          AND (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
               WHERE A.consignment_id = C.local_id AND A.target_type = 'TOPAY_CONSIGNMENT' AND A.deleted_at IS NULL) = 0
        """,
    )
    suspend fun topayPendingTotal(companyId: String): Long

    @Query(
        """
        SELECT COALESCE(SUM(T.hire_paise), 0) FROM TRIP_E T
        WHERE T.company_id = :companyId AND T.deleted_at IS NULL
          AND T.created_at >= :from AND T.created_at <= :to
        """,
    )
    suspend fun lorryHireTotal(companyId: String, from: Long, to: Long): Long

    /** Cancellation rows — the "No gaps" chip's basis (§B21). */
    @Query(
        """
        SELECT COUNT(*) FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND status_projection = 'CANCELLED' AND booked_at >= :from AND booked_at <= :to
        """,
    )
    suspend fun numberGaps(companyId: String, from: Long, to: Long): Long

    @Query(
        """
        SELECT OS.name || ' → ' || DS.name AS route, COALESCE(SUM(C.freight_paise), 0) AS freightPaise, COUNT(*) AS cnt
        FROM CONSIGNMENT_E C
        JOIN STATION_E OS ON OS.local_id = C.from_station_id
        JOIN STATION_E DS ON DS.local_id = C.to_station_id
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL
          AND C.status_projection NOT IN ('DRAFT', 'CANCELLED')
          AND C.booked_at >= :from AND C.booked_at <= :to
        GROUP BY OS.name, DS.name
        ORDER BY freightPaise DESC
        LIMIT 20
        """,
    )
    suspend fun revenueByRoute(companyId: String, from: Long, to: Long): List<RouteRevenueRow>

    @Query(
        """
        SELECT P.name AS party,
               COALESCE(SUM(B.total_paise - (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
                    WHERE A.bill_id = B.local_id AND A.target_type = 'BILL' AND A.deleted_at IS NULL)), 0) AS outstandingPaise,
               COALESCE(SUM(CASE WHEN :now - COALESCE(B.issued_at, 0) > 90 * 86400000
                    THEN B.total_paise - (SELECT COALESCE(SUM(A2.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A2
                         WHERE A2.bill_id = B.local_id AND A2.target_type = 'BILL' AND A2.deleted_at IS NULL)
                    ELSE 0 END), 0) AS over90Paise
        FROM FREIGHT_BILL_E B JOIN PARTY_E P ON P.local_id = B.party_id
        WHERE B.company_id = :companyId AND B.state = 'ISSUED' AND B.deleted_at IS NULL
        GROUP BY P.name
        HAVING outstandingPaise > 0
        ORDER BY outstandingPaise DESC
        """,
    )
    suspend fun outstandingByParty(companyId: String, now: Long): List<PartyOutstandingRow>

    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM CONSIGNMENT_E C WHERE C.company_id = :companyId AND C.deleted_at IS NULL AND C.status_projection NOT IN ('DRAFT','CANCELLED')) AS register,
            (SELECT COUNT(*) FROM CHARGE_LINE_E L JOIN CONSIGNMENT_E C ON C.local_id = L.consignment_id WHERE C.company_id = :companyId AND L.deleted_at IS NULL) AS chargeLines,
            (SELECT COUNT(*) FROM FREIGHT_BILL_E B WHERE B.company_id = :companyId AND B.deleted_at IS NULL) AS bills,
            (SELECT COUNT(*) FROM RECEIPT_E R WHERE R.company_id = :companyId AND R.deleted_at IS NULL) AS receipts,
            (SELECT COUNT(*) FROM RECEIPT_ALLOCATION_E A WHERE A.company_id = :companyId AND A.deleted_at IS NULL) AS allocations,
            (SELECT COUNT(*) FROM CONSIGNMENT_E C2 WHERE C2.company_id = :companyId AND C2.deleted_at IS NULL AND C2.payment_mode = 'TOPAY') AS topayCollections,
            (SELECT COUNT(*) FROM TRIP_E T WHERE T.company_id = :companyId AND T.deleted_at IS NULL AND T.challan_no IS NOT NULL) AS trips,
            (SELECT COUNT(*) FROM PARTY_E P WHERE P.company_id = :companyId AND P.deleted_at IS NULL) AS parties
        """,
    )
    suspend fun sheetCounts(companyId: String): SheetCountsRow
}
