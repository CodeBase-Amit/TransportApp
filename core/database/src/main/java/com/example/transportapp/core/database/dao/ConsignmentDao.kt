package com.example.transportapp.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.transportapp.core.database.entity.ChargeLineEntity
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.entity.ConsignmentItemEntity
import com.example.transportapp.core.database.entity.DocSnapshotEntity
import com.example.transportapp.core.database.entity.StatusEventEntity

/** One register docket row: the consignment joined to the names it prints (Phase2.md S6). */
data class RegisterDocketRow(
    val local_id: String,
    val display_no: String,
    val consignee_name: String,
    val total_paise: Long,
    val from_station: String,
    val to_station: String,
    val status: String,
    val payment_mode: String,
    val packages: Long,
    val weight_kg: Long,
    val booked_at: Long,
    val sync_state: String,
    val held_remark: String?,
)

/** The summary strip's three aggregates over the same filter as the list (Design T7). */
data class RegisterSummaryRow(
    val matching: Int,
    val packages: Long,
    val amountPaise: Long,
)

/** One case-file timeline event (T8 §WHERE IT IS). */
data class CaseEventRow(
    val event_type: String,
    val location: String?,
    val occurred_at: Long,
    val actor_name: String,
    val remark: String?,
    val challan_ref: String?,
)

/**
 * Consignment access (Phase2.md S5). Writes are upserts; reads filter tombstones.
 * STATUS_EVENT_E has no update or delete path — the log is append-only (§3.4 #7).
 */
@Dao
interface ConsignmentDao {

    @Upsert
    suspend fun upsertConsignment(entity: ConsignmentEntity)

    @Query("SELECT * FROM CONSIGNMENT_E WHERE company_id = :companyId AND bilty_no = :biltyNo AND deleted_at IS NULL")
    suspend fun getConsignmentByBiltyNo(companyId: String, biltyNo: String): ConsignmentEntity?

    /** Provisional-number lookup — the T6/T8 cross-reference path (§9). */
    @Query("SELECT * FROM CONSIGNMENT_E WHERE company_id = :companyId AND provisional_no = :provisionalNo AND deleted_at IS NULL")
    suspend fun getConsignmentByProvisionalNo(companyId: String, provisionalNo: String): ConsignmentEntity?

    @Query("SELECT * FROM CONSIGNMENT_E WHERE local_id = :localId AND deleted_at IS NULL")
    suspend fun getConsignment(localId: String): ConsignmentEntity?

    @Query("SELECT COUNT(*) FROM CONSIGNMENT_E WHERE company_id = :companyId AND deleted_at IS NULL")
    suspend fun countConsignments(companyId: String): Int

    // ── T7 register (Phase2.md S6, D6: Paging 3 only here) ──────────────
    // D7 amendment: search is a bounded LIKE on the number/party denorm/private mark;
    // CONSIGNMENT_FTS stays for the sync phase.

    @Query(
        """
        SELECT
            C.local_id AS local_id,
            CASE WHEN C.bilty_no IS NOT NULL THEN C.bilty_no ELSE C.provisional_no END AS display_no,
            P.name AS consignee_name,
            C.total_paise AS total_paise,
            OS.name AS from_station,
            DS.name AS to_station,
            C.status_projection AS status,
            C.payment_mode AS payment_mode,
            C.packages AS packages,
            C.chargeable_weight_g / 1000 AS weight_kg,
            C.booked_at AS booked_at,
            C.sync_state AS sync_state,
            (
                SELECT E.remark FROM STATUS_EVENT_E E
                WHERE E.consignment_id = C.local_id AND E.event_type = 'HELD' AND E.deleted_at IS NULL
                ORDER BY E.occurred_at DESC LIMIT 1
            ) AS held_remark
        FROM CONSIGNMENT_E C
        JOIN PARTY_E P ON P.local_id = C.consignee_id
        JOIN STATION_E OS ON OS.local_id = C.from_station_id
        JOIN STATION_E DS ON DS.local_id = C.to_station_id
        WHERE C.company_id = :companyId
          AND C.deleted_at IS NULL
          AND C.status_projection != 'DRAFT'
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
          AND (:status IS NULL OR C.status_projection = :status)
          AND (:paymentMode IS NULL OR C.payment_mode = :paymentMode)
          AND (:unbilledOnly = 0 OR C.freight_bill_id IS NULL)
          AND (:sinceAt IS NULL OR C.booked_at >= :sinceAt)
          AND (
              :pattern IS NULL
              OR C.bilty_no LIKE :pattern
              OR C.provisional_no LIKE :pattern
              OR C.party_names LIKE :pattern
              OR C.private_mark LIKE :pattern
          )
        ORDER BY C.booked_at DESC
        """,
    )
    fun pagingRegister(
        companyId: String,
        branchId: String?,
        status: String?,
        paymentMode: String?,
        unbilledOnly: Boolean,
        sinceAt: Long?,
        pattern: String?,
    ): PagingSource<Int, RegisterDocketRow>

    @Query(
        """
        SELECT
            COUNT(*) AS matching,
            COALESCE(SUM(C.packages), 0) AS packages,
            COALESCE(SUM(C.total_paise), 0) AS amountPaise
        FROM CONSIGNMENT_E C
        WHERE C.company_id = :companyId
          AND C.deleted_at IS NULL
          AND C.status_projection != 'DRAFT'
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
          AND (:status IS NULL OR C.status_projection = :status)
          AND (:paymentMode IS NULL OR C.payment_mode = :paymentMode)
          AND (:unbilledOnly = 0 OR C.freight_bill_id IS NULL)
          AND (:sinceAt IS NULL OR C.booked_at >= :sinceAt)
          AND (
              :pattern IS NULL
              OR C.bilty_no LIKE :pattern
              OR C.provisional_no LIKE :pattern
              OR C.party_names LIKE :pattern
              OR C.private_mark LIKE :pattern
          )
        """,
    )
    suspend fun summaryRegister(
        companyId: String,
        branchId: String?,
        status: String?,
        paymentMode: String?,
        unbilledOnly: Boolean,
        sinceAt: Long?,
        pattern: String?,
    ): RegisterSummaryRow

    @Upsert
    suspend fun upsertItem(entity: ConsignmentItemEntity)

    @Query("SELECT * FROM CONSIGNMENT_ITEM_E WHERE consignment_id = :consignmentId AND deleted_at IS NULL")
    suspend fun getItems(consignmentId: String): List<ConsignmentItemEntity>

    @Upsert
    suspend fun upsertChargeLine(entity: ChargeLineEntity)

    @Query("SELECT * FROM CHARGE_LINE_E WHERE consignment_id = :consignmentId AND deleted_at IS NULL ORDER BY sort_order")
    suspend fun getChargeLines(consignmentId: String): List<ChargeLineEntity>

    /** Append-only: insert only. There is no update or delete for events by design (§7.2). */
    @Upsert
    suspend fun insertStatusEvent(entity: StatusEventEntity)

    @Query("SELECT * FROM STATUS_EVENT_E WHERE consignment_id = :consignmentId AND deleted_at IS NULL ORDER BY occurred_at, local_id")
    suspend fun getEvents(consignmentId: String): List<StatusEventEntity>

    @Upsert
    suspend fun upsertSnapshot(entity: DocSnapshotEntity)

    @Query(
        """
        SELECT * FROM DOC_SNAPSHOT_E
        WHERE consignment_id = :consignmentId AND deleted_at IS NULL
        ORDER BY version DESC LIMIT 1
        """,
    )
    suspend fun getLatestSnapshot(consignmentId: String): DocSnapshotEntity?

    @Query("SELECT * FROM DOC_SNAPSHOT_E WHERE consignment_id = :consignmentId AND deleted_at IS NULL ORDER BY version")
    suspend fun getSnapshots(consignmentId: String): List<DocSnapshotEntity>

    // ── S8: attachments and POD (§4.1) ──────────────────────────────────

    @Upsert
    suspend fun upsertAttachment(entity: com.example.transportapp.core.database.entity.AttachmentEntity)

    @Query("SELECT * FROM ATTACHMENT_E WHERE consignment_id = :consignmentId AND deleted_at IS NULL")
    suspend fun getAttachments(consignmentId: String): List<com.example.transportapp.core.database.entity.AttachmentEntity>

    @Upsert
    suspend fun upsertPod(entity: com.example.transportapp.core.database.entity.PodEntity)

    @Query("SELECT * FROM POD_E WHERE consignment_id = :consignmentId AND deleted_at IS NULL LIMIT 1")
    suspend fun getPod(consignmentId: String): com.example.transportapp.core.database.entity.PodEntity?

    @Query(
        """
        SELECT COUNT(*) FROM CONSIGNMENT_E C
        WHERE C.company_id = :companyId AND C.deleted_at IS NULL
          AND C.status_projection NOT IN ('DELIVERED', 'CANCELLED', 'RETURNED')
          AND C.expected_arrival < :overdueBefore
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
        """,
    )
    suspend fun countOverdue(companyId: String, branchId: String?, overdueBefore: Long): Int

    /** Held events in the last window — the T4 exception strip (§13). */
    @Query(
        """
        SELECT E.* FROM STATUS_EVENT_E E
        JOIN CONSIGNMENT_E C ON C.local_id = E.consignment_id
        WHERE E.company_id = :companyId AND E.deleted_at IS NULL
          AND E.event_type = 'HELD' AND E.occurred_at >= :sinceAt
          AND (:branchId IS NULL OR C.booking_branch_id = :branchId)
        ORDER BY E.occurred_at DESC
        LIMIT 10
        """,
    )
    suspend fun getRecentHeldEvents(companyId: String, branchId: String?, sinceAt: Long): List<StatusEventEntity>

    /** The latest event per consignment, for rebuild and ageing reads. */
    @Query(
        """
        SELECT * FROM STATUS_EVENT_E
        WHERE consignment_id = :consignmentId AND deleted_at IS NULL
        ORDER BY occurred_at, local_id
        """,
    )
    suspend fun getEventsOrdered(consignmentId: String): List<StatusEventEntity>
}
