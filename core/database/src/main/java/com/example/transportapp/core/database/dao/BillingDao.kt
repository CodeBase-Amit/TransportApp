package com.example.transportapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.FreightBillEntity
import com.example.transportapp.core.database.entity.ReceiptAllocationEntity
import com.example.transportapp.core.database.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow

/** One T13 party-group card (§12.1's unbilled pool grouped by party). */
data class UnbilledPartyRow(
    val party_id: String,
    val party_name: String,
    val gstin: String?,
    val consignments: Int,
    val freight_paise: Long,
    val total_paise: Long,
    /** Σ total for consignments aged 0–30 / 31–60 / 60+ days — the ageing bar segments. */
    val bucket0_30_paise: Long,
    val bucket31_60_paise: Long,
    val bucket60plus_paise: Long,
    val oldest_days: Long,
    val first_booked_at: Long,
    val last_booked_at: Long,
)

/** One T13 expanded consignment row. */
data class UnbilledConsignmentRow(
    val local_id: String,
    val display_no: String,
    val total_paise: Long,
    val booked_at: Long,
    val from_station: String,
    val to_station: String,
)

/** One T15 To Pay row awaiting collection. */
data class TopayRow(
    val local_id: String,
    val display_no: String,
    val consignee_id: String,
    val consignee_name: String,
    val total_paise: Long,
    val status: String,
    val held_remark: String?,
    val waived: Boolean,
)

/** One T15 tab-2 receipt row. */
data class ReceiptRow(
    val local_id: String,
    val receipt_no: String?,
    val party_name: String,
    val instrument: String,
    val instrument_ref: String?,
    val received_at: Long,
    val amount_paise: Long,
)

/** Outstanding issued bill for the allocation sheet / statement. */
data class OutstandingBillRow(
    val local_id: String,
    val bill_no: String?,
    val total_paise: Long,
    val issued_at: Long?,
    val due_at: Long?,
    val allocated_paise: Long,
)

/**
 * Money reads and writes (Phase2.md S9). Writes are upserts; reads filter tombstones.
 * The one-consignment-one-live-bill rule is repository-enforced (§12.1) — the DAO exposes
 * the count query the transaction checks.
 */
@Dao
interface BillingDao {

    // ── Freight bills ───────────────────────────────────────────────────

    @Upsert
    suspend fun upsertBill(entity: FreightBillEntity)

    @Query("SELECT * FROM FREIGHT_BILL_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getBill(localId: String): FreightBillEntity?

    @Query("SELECT * FROM FREIGHT_BILL_E WHERE local_id = :localId AND deleted_at IS NULL")
    fun observeBill(localId: String): Flow<FreightBillEntity?>

    @Query("SELECT * FROM FREIGHT_BILL_E WHERE company_id = :companyId AND party_id = :partyId AND state = 'ISSUED' AND deleted_at IS NULL ORDER BY issued_at DESC")
    fun observeIssuedBillsForParty(companyId: String, partyId: String): Flow<List<FreightBillEntity>>

    @Query("SELECT COUNT(*) FROM FREIGHT_BILL_E WHERE company_id = :companyId AND bill_no = :billNo AND deleted_at IS NULL")
    suspend fun countBillsByNo(companyId: String, billNo: String): Int

    /** T14 draft header + T16: the party as the bill knows it. */
    @Query(
        """
        SELECT B.*, P.name AS party_name, P.gstin AS party_gstin
        FROM FREIGHT_BILL_E B JOIN PARTY_E P ON P.local_id = B.party_id
        WHERE B.local_id = :billId AND B.deleted_at IS NULL
        """,
    )
    suspend fun getBillWithParty(billId: String): BillWithParty?

    @Query(
        """
        SELECT B.*, P.name AS party_name, P.gstin AS party_gstin
        FROM FREIGHT_BILL_E B JOIN PARTY_E P ON P.local_id = B.party_id
        WHERE B.local_id = :billId AND B.deleted_at IS NULL
        """,
    )
    fun observeBillWithParty(billId: String): Flow<BillWithParty?>

    @Query(
        """
        SELECT
            C.local_id AS local_id,
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            C.total_paise AS total_paise,
            C.booked_at AS booked_at,
            OS.name AS from_station,
            DS.name AS to_station
        FROM CONSIGNMENT_E C
        JOIN STATION_E OS ON OS.local_id = C.from_station_id
        JOIN STATION_E DS ON DS.local_id = C.to_station_id
        WHERE C.freight_bill_id = :billId AND C.deleted_at IS NULL
        ORDER BY C.booked_at
        """,
    )
    suspend fun getBillConsignments(billId: String): List<UnbilledConsignmentRow>

    @Query(
        """
        SELECT
            C.local_id AS local_id,
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            C.total_paise AS total_paise,
            C.booked_at AS booked_at,
            OS.name AS from_station,
            DS.name AS to_station
        FROM CONSIGNMENT_E C
        JOIN STATION_E OS ON OS.local_id = C.from_station_id
        JOIN STATION_E DS ON DS.local_id = C.to_station_id
        WHERE C.freight_bill_id = :billId AND C.deleted_at IS NULL
        ORDER BY C.booked_at
        """,
    )
    fun observeBillConsignments(billId: String): Flow<List<UnbilledConsignmentRow>>

    // ── Unbilled pool (T13) ─────────────────────────────────────────────

    @Query(
        """
        SELECT
            C.consignor_id AS party_id,
            P.name AS party_name,
            P.gstin AS gstin,
            COUNT(*) AS consignments,
            SUM(C.freight_paise) AS freight_paise,
            SUM(C.total_paise) AS total_paise,
            SUM(CASE WHEN :now - C.booked_at <= 30 * 86400000 THEN C.total_paise ELSE 0 END) AS bucket0_30_paise,
            SUM(CASE WHEN :now - C.booked_at > 30 * 86400000 AND :now - C.booked_at <= 60 * 86400000 THEN C.total_paise ELSE 0 END) AS bucket31_60_paise,
            SUM(CASE WHEN :now - C.booked_at > 60 * 86400000 THEN C.total_paise ELSE 0 END) AS bucket60plus_paise,
            MAX((:now - C.booked_at) / 86400000) AS oldest_days,
            MIN(C.booked_at) AS first_booked_at,
            MAX(C.booked_at) AS last_booked_at
        FROM CONSIGNMENT_E C
        JOIN PARTY_E P ON P.local_id = C.consignor_id
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL
          AND C.payment_mode = 'TBB'
          AND C.freight_bill_id IS NULL
          AND C.status_projection NOT IN ('CANCELLED')
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
          AND (:sinceAt IS NULL OR C.booked_at >= :sinceAt)
          AND (:agedBefore IS NULL OR C.booked_at <= :agedBefore)
        GROUP BY C.consignor_id
        ORDER BY total_paise DESC
        """,
    )
    fun observeUnbilledPool(companyId: String, branchId: String?, sinceAt: Long?, agedBefore: Long?, now: Long): Flow<List<UnbilledPartyRow>>

    @Query(
        """
        SELECT
            C.local_id AS local_id,
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            C.total_paise AS total_paise,
            C.booked_at AS booked_at,
            OS.name AS from_station,
            DS.name AS to_station
        FROM CONSIGNMENT_E C
        JOIN STATION_E OS ON OS.local_id = C.from_station_id
        JOIN STATION_E DS ON DS.local_id = C.to_station_id
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL
          AND C.payment_mode = 'TBB'
          AND C.consignor_id = :partyId
          AND C.freight_bill_id IS NULL
          AND C.status_projection NOT IN ('CANCELLED')
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
          AND (:sinceAt IS NULL OR C.booked_at >= :sinceAt)
          AND (:agedBefore IS NULL OR C.booked_at <= :agedBefore)
        ORDER BY C.booked_at DESC
        """,
    )
    fun observeUnbilledForParty(companyId: String, partyId: String, branchId: String?, sinceAt: Long?, agedBefore: Long?): Flow<List<UnbilledConsignmentRow>>

    /** The §12.1 pool check the draft transaction runs: none of these may already be on a live bill. */
    @Query(
        """
        SELECT COUNT(*) FROM CONSIGNMENT_E
        WHERE company_id = :companyId AND deleted_at IS NULL
          AND local_id IN (:consignmentIds)
          AND freight_bill_id IS NOT NULL
        """,
    )
    suspend fun countAlreadyBilled(companyId: String, consignmentIds: List<String>): Int

    // ── Receipts + allocations (T15) ────────────────────────────────────

    @Upsert
    suspend fun upsertReceipt(entity: ReceiptEntity)

    @Query("SELECT * FROM RECEIPT_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getReceipt(localId: String): ReceiptEntity?

    @Upsert
    suspend fun upsertAllocation(entity: ReceiptAllocationEntity)

    @Query("SELECT * FROM RECEIPT_ALLOCATION_E WHERE receipt_id = :receiptId AND deleted_at IS NULL")
    suspend fun getAllocations(receiptId: String): List<ReceiptAllocationEntity>

    @Query("SELECT COALESCE(SUM(amount_paise), 0) FROM RECEIPT_ALLOCATION_E WHERE consignment_id = :consignmentId AND target_type = 'TOPAY_CONSIGNMENT' AND deleted_at IS NULL")
    suspend fun sumTopayAllocated(consignmentId: String): Long

    @Query(
        """
        SELECT
            C.local_id AS local_id,
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            C.consignee_id AS consignee_id,
            CN.name AS consignee_name,
            C.total_paise AS total_paise,
            C.status_projection AS status,
            (
                SELECT E.remark FROM STATUS_EVENT_E E
                WHERE E.consignment_id = C.local_id AND E.event_type = 'HELD' AND E.deleted_at IS NULL
                ORDER BY E.occurred_at DESC LIMIT 1
            ) AS held_remark,
            EXISTS (
                SELECT 1 FROM STATUS_EVENT_E E
                WHERE E.consignment_id = C.local_id AND E.event_type = 'WAIVE_TOPAY' AND E.deleted_at IS NULL
            ) AS waived
        FROM CONSIGNMENT_E C
        JOIN PARTY_E CN ON CN.local_id = C.consignee_id
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL
          AND C.payment_mode = 'TOPAY'
          AND C.status_projection NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')
          AND C.booking_branch_id = :branchId
          AND (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
               WHERE A.consignment_id = C.local_id AND A.target_type = 'TOPAY_CONSIGNMENT' AND A.deleted_at IS NULL) = 0
        ORDER BY C.booked_at DESC
        """,
    )
    fun observeTopayAwaiting(companyId: String, branchId: String): Flow<List<TopayRow>>

    /** Parties the allocation sheet can pick: anyone with an issued bill outstanding. */
    @Query(
        """
        SELECT DISTINCT P.local_id AS party_id, P.name AS party_name
        FROM FREIGHT_BILL_E B JOIN PARTY_E P ON P.local_id = B.party_id
        WHERE B.company_id = :companyId AND B.state = 'ISSUED' AND B.deleted_at IS NULL
        ORDER BY P.name
        """,
    )
    suspend fun getPartiesWithIssuedBills(companyId: String): List<PartyOption>

    @Query(
        """
        SELECT
            R.local_id AS local_id,
            R.receipt_no AS receipt_no,
            P.name AS party_name,
            R.instrument AS instrument,
            R.instrument_ref AS instrument_ref,
            R.received_at AS received_at,
            R.amount_paise AS amount_paise
        FROM RECEIPT_E R
        JOIN PARTY_E P ON P.local_id = R.party_id
        WHERE R.company_id = :companyId AND R.deleted_at IS NULL
          AND (:branchId IS NULL OR R.received_at_branch_id = :branchId)
          AND (:sinceAt IS NULL OR R.received_at >= :sinceAt)
        ORDER BY R.received_at DESC
        """,
    )
    fun observeReceipts(companyId: String, branchId: String?, sinceAt: Long?): Flow<List<ReceiptRow>>

    /** Issued, uncancelled bills with their allocation totals — the allocation sheet's targets. */
    @Query(
        """
        SELECT
            B.local_id AS local_id,
            B.bill_no AS bill_no,
            B.total_paise AS total_paise,
            B.issued_at AS issued_at,
            B.due_at AS due_at,
            (SELECT COALESCE(SUM(A.amount_paise), 0) FROM RECEIPT_ALLOCATION_E A
             WHERE A.bill_id = B.local_id AND A.target_type = 'BILL' AND A.deleted_at IS NULL) AS allocated_paise
        FROM FREIGHT_BILL_E B
        WHERE B.company_id = :companyId AND B.party_id = :partyId
          AND B.state = 'ISSUED' AND B.deleted_at IS NULL
        ORDER BY B.issued_at DESC
        """,
    )
    suspend fun getOutstandingBills(companyId: String, partyId: String): List<OutstandingBillRow>

    @Query("SELECT COALESCE(SUM(amount_paise), 0) FROM RECEIPT_ALLOCATION_E WHERE bill_id = :billId AND target_type = 'BILL' AND deleted_at IS NULL")
    suspend fun sumBillAllocated(billId: String): Long

    @Query(
        """
        SELECT COALESCE(SUM(R.amount_paise), 0) FROM RECEIPT_E R
        WHERE R.company_id = :companyId AND R.deleted_at IS NULL
          AND (:branchId IS NULL OR R.received_at_branch_id = :branchId)
          AND R.received_at >= :sinceAt
        """,
    )
    suspend fun sumReceiptsSince(companyId: String, branchId: String?, sinceAt: Long): Long

    @Query(
        """
        SELECT COUNT(*) FROM RECEIPT_E R
        WHERE R.company_id = :companyId AND R.deleted_at IS NULL
          AND (:branchId IS NULL OR R.received_at_branch_id = :branchId)
          AND R.received_at >= :sinceAt
        """,
    )
    suspend fun countReceiptsSince(companyId: String, branchId: String?, sinceAt: Long): Int

    // ── Statement (T16, §12.3) ──────────────────────────────────────────

    @Query("SELECT * FROM FREIGHT_BILL_E WHERE company_id = :companyId AND party_id = :partyId AND state = 'ISSUED' AND deleted_at IS NULL ORDER BY COALESCE(issued_at, updated_at_local)")
    suspend fun getIssuedBillsForParty(companyId: String, partyId: String): List<FreightBillEntity>

    @Query("SELECT * FROM RECEIPT_E WHERE company_id = :companyId AND party_id = :partyId AND deleted_at IS NULL ORDER BY received_at")
    suspend fun getReceiptsForParty(companyId: String, partyId: String): List<ReceiptEntity>

    @Query("SELECT * FROM CREDIT_NOTE_E WHERE company_id = :companyId AND party_id = :partyId AND deleted_at IS NULL ORDER BY created_at")
    suspend fun getCreditNotesForParty(companyId: String, partyId: String): List<com.example.transportapp.core.database.entity.CreditNoteEntity>

    @Query("SELECT * FROM RECEIPT_ALLOCATION_E WHERE deleted_at IS NULL AND bill_id IN (:billIds)")
    suspend fun getAllocationsForBills(billIds: List<String>): List<ReceiptAllocationEntity>

    @Query("SELECT * FROM RECEIPT_ALLOCATION_E WHERE deleted_at IS NULL AND receipt_id IN (:receiptIds)")
    suspend fun getAllocationsForReceipts(receiptIds: List<String>): List<ReceiptAllocationEntity>

    @Query("SELECT name, gstin FROM PARTY_E WHERE local_id = :partyId AND deleted_at IS NULL")
    suspend fun getPartyNameGstin(partyId: String): PartyNameGstin?
}

/** T14/T16 party block. */
data class BillWithParty(
    val local_id: String,
    val bill_no: String?,
    val state: String,
    val party_id: String,
    val party_name: String,
    val party_gstin: String?,
    val period_start: Long,
    val period_end: Long,
    val due_at: Long?,
    val freight_paise: Long,
    val other_charges_paise: Long,
    val taxable_paise: Long,
    val gst_paise: Long,
    val total_paise: Long,
    val gst_treatment: String,
    val notes: String?,
    val issued_at: Long?,
    val issued_by_name: String?,
    val cancelled_at: Long?,
)

data class PartyNameGstin(val name: String, val gstin: String?)

data class PartyOption(val party_id: String, val party_name: String)
