package com.example.transportapp.data.transport.billing

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.dao.BillWithParty
import com.example.transportapp.core.database.dao.PartyOption
import com.example.transportapp.core.database.dao.UnbilledConsignmentRow
import com.example.transportapp.core.database.dao.UnbilledPartyRow
import com.example.transportapp.core.database.entity.FreightBillEntity
import com.example.transportapp.core.database.entity.ReceiptAllocationEntity
import com.example.transportapp.core.database.entity.ReceiptEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.numbering.NumberingRepository
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.rate.GstinStateCodes
import com.example.transportapp.data.transport.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** T13's filter chips: scope, period and age are independent toggles. */
data class PoolFilter(
    /** Billing is company-level (D43): the pool defaults to all branches. */
    val allBranches: Boolean = true,
    val thisQuarterOnly: Boolean = true,
    val minAgeDays: Int? = null,
)

/** One T13 party-group card. */
data class UnbilledPartyGroup(
    val partyId: String,
    val partyName: String,
    val gstin: String?,
    val consignments: Int,
    val freightPaise: Long,
    val totalPaise: Long,
    val bucket0to30Paise: Long,
    val bucket31to60Paise: Long,
    val bucket60plusPaise: Long,
    val oldestDays: Long,
    val firstBookedAt: Long,
    val lastBookedAt: Long,
)

internal fun UnbilledPartyRow.toGroup() = UnbilledPartyGroup(
    partyId = party_id, partyName = party_name, gstin = gstin,
    consignments = consignments, freightPaise = freight_paise, totalPaise = total_paise,
    bucket0to30Paise = bucket0_30_paise, bucket31to60Paise = bucket31_60_paise,
    bucket60plusPaise = bucket60plus_paise, oldestDays = oldest_days,
    firstBookedAt = first_booked_at, lastBookedAt = last_booked_at,
)

/** One T14 consignment line (also the T13 expanded row). */
data class BillConsignmentLine(
    val localId: String,
    val displayNo: String,
    val totalPaise: Long,
    val bookedAt: Long,
    val fromStation: String,
    val toStation: String,
)

internal fun UnbilledConsignmentRow.toLine() = BillConsignmentLine(local_id, display_no, total_paise, booked_at, from_station, to_station)

/** The bill header as screens see it (DB types stay internal to the data layer). */
data class BillHeader(
    val localId: String,
    val billNo: String?,
    val state: String,
    val partyId: String,
    val partyName: String,
    val partyGstin: String?,
    val periodStart: Long,
    val periodEnd: Long,
    val dueAt: Long?,
    val freightPaise: Long,
    val otherChargesPaise: Long,
    val taxablePaise: Long,
    val gstPaise: Long,
    val totalPaise: Long,
    val gstTreatment: String,
    val notes: String?,
    val issuedAt: Long?,
    val issuedByName: String?,
    val cancelledAt: Long?,
)

/** T14: the bill as one read model — header, rows, and what is still owed. */
data class BillDetail(
    val bill: BillHeader,
    val rows: List<BillConsignmentLine>,
    val outstandingPaise: Long,
)

/** T15 tab 1 row. */
data class TopayLine(
    val localId: String,
    val displayNo: String,
    val consigneePartyId: String,
    val consigneeName: String,
    val amountPaise: Long,
    val status: String,
    val heldRemark: String?,
    val waived: Boolean,
) {
    val collectable: Boolean get() = status != "HELD" || waived
}

/** T15 tab 2 row. */
data class ReceiptLine(
    val localId: String,
    val receiptNo: String?,
    val partyName: String,
    val instrument: String,
    val instrumentRef: String?,
    val receivedAt: Long,
    val amountPaise: Long,
)

/** One row of the allocation sheet. */
data class OutstandingBill(
    val localId: String,
    val billNo: String?,
    val totalPaise: Long,
    val issuedAt: Long?,
    val dueAt: Long?,
    val outstandingPaise: Long,
)

/** The T15 "Collect" / "Record a receipt" inputs. */
data class AllocationInput(
    /** BILL / TOPAY_CONSIGNMENT / ON_ACCOUNT. */
    val targetType: String,
    val billId: String? = null,
    val consignmentId: String? = null,
    val amountPaise: Long,
)

/** T16's §12.3 statement: opening, chronological ledger, closing, ageing. */
data class Statement(
    val partyId: String,
    val partyName: String,
    val gstin: String?,
    val periodStart: Long,
    val periodEnd: Long,
    /** Debit-positive running balance before the period's first row. */
    val openingPaise: Long,
    val rows: List<StatementRow>,
    /** Debit positive ("Dr"); negative renders as "Cr" — on-account credits can push it there. */
    val closingPaise: Long,
    /** The outstanding portion of issued bills older than 90 days. */
    val over90Paise: Long,
)

data class StatementRow(
    val at: Long,
    val docNo: String,
    val description: String,
    val debitPaise: Long,
    val creditPaise: Long,
    val balancePaise: Long,
)

/** A saved receipt as screens/tests see it (DB types stay internal to the data layer). */
data class ReceiptSaved(
    val localId: String,
    val receiptNo: String?,
    val amountPaise: Long,
)

/** A pickable party for the allocation sheet (DB types stay internal to the data layer). */
data class PartyChoice(
    val partyId: String,
    val partyName: String,
)

/**
 * Billing, To Pay and receipts (Phase2.md S9, §12). The three money paths stay separate:
 * TBB consignments flow into freight bills; To Pay collections target the consignment; a
 * Paid booking needs neither. Bill *issue* is a server guarantee (one consignment on one
 * live bill), so Phase 2 keeps it draft-only and answers OFFLINE_UNAVAILABLE.
 */
interface BillingRepository {

    fun observeUnbilledPool(filter: PoolFilter): Flow<List<UnbilledPartyGroup>>

    fun observeUnbilledRows(partyId: String, filter: PoolFilter): Flow<List<BillConsignmentLine>>

    suspend fun buildDraftBill(partyId: String, consignmentIds: List<String>, dueAt: Long?, notes: String?, now: Long): Result<BillHeader>

    /** A draft is mutable; removing a line returns the consignment to the pool (§12.1). */
    suspend fun removeConsignmentFromDraft(billId: String, consignmentId: String, now: Long): Result<Unit>

    suspend fun cancelBill(billId: String, now: Long): Result<Unit>

    /** Draft-only in Phase 2 (§1): the number is consumed server-side, so this stays refused. */
    suspend fun issueBill(billId: String, now: Long): Result<Unit>

    fun observeBill(billId: String): Flow<BillDetail?>

    fun observeTopayAwaiting(): Flow<List<TopayLine>>

    fun observeRecentReceipts(): Flow<List<ReceiptLine>>

    suspend fun receiptsSinceSummary(sinceAt: Long): Pair<Long, Int>

    suspend fun recordReceipt(payerPartyId: String, amountPaise: Long, instrument: String, instrumentRef: String?, allocations: List<AllocationInput>, now: Long): Result<ReceiptSaved>

    suspend fun outstandingBillsForParty(partyId: String): List<OutstandingBill>

    suspend fun partiesWithIssuedBills(): List<PartyChoice>

    suspend fun statement(partyId: String, periodStart: Long, periodEnd: Long, now: Long): Result<Statement>
}

@Singleton
class BillingRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val sessionRepository: SessionRepository,
    private val numberingRepository: NumberingRepository,
    private val outboxWriter: OutboxWriter,
) : BillingRepository {

    override fun observeUnbilledPool(filter: PoolFilter): Flow<List<UnbilledPartyGroup>> {
        val (sinceAt, agedBefore) = periodBounds(filter)
        return flow {
            val session = sessionRepository.session.first()
            emitAll(
                database.billingDao()
                    .observeUnbilledPool(session.companyId, branchId(filter), sinceAt, agedBefore, System.currentTimeMillis())
                    .map { rows -> rows.map { it.toGroup() } },
            )
        }
    }

    override fun observeUnbilledRows(partyId: String, filter: PoolFilter): Flow<List<BillConsignmentLine>> {
        val (sinceAt, agedBefore) = periodBounds(filter)
        return flow {
            val session = sessionRepository.session.first()
            emitAll(
                database.billingDao()
                    .observeUnbilledForParty(session.companyId, partyId, branchId(filter), sinceAt, agedBefore)
                    .map { rows -> rows.map { it.toLine() } },
            )
        }
    }

    override suspend fun buildDraftBill(partyId: String, consignmentIds: List<String>, dueAt: Long?, notes: String?, now: Long): Result<BillHeader> = database.withTransaction {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        if (consignmentIds.isEmpty()) return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Select at least one consignment")

        val consignments = consignmentIds.mapNotNull { database.consignmentDao().getConsignment(it) }
        if (consignments.size != consignmentIds.size) {
            return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "A selected consignment is not on this device")
        }
        consignments.forEach { c ->
            if (c.company_id != session.companyId || c.consignor_id != partyId) {
                return@withTransaction Result.failure(ErrorCode.TENANT_MISMATCH, "A selected consignment does not belong to this party")
            }
            if (c.freight_bill_id != null) {
                return@withTransaction Result.failure(ErrorCode.ALREADY_BILLED, "One of the consignments is already on a live bill")
            }
            if (c.payment_mode != "TBB") {
                return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Only To Be Billed consignments go on a freight bill")
            }
        }

        // §12.1: a bill freezes one GST treatment. The treatment is where each consignment's
        // place of supply sits relative to the company's registered state — never the stations.
        val company = database.orgDao().getCompany(session.companyId)
            ?: return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active company")
        val registeredState = company.gstin?.let { GstinStateCodes.stateOf(it) }
        val treatments = consignments.map { c ->
            val supply = c.place_of_supply_state
            val intra = supply != null && registeredState != null && supply == registeredState
            if (intra) "Intra-state (CGST + SGST)" else "Inter-state (IGST)"
        }.distinct()
        if (treatments.size > 1) {
            return@withTransaction Result.failure(
                ErrorCode.BILL_MIXED_TREATMENT,
                "The selected consignments carry ${treatments.joinToString(" and ")} — they have to go on separate bills",
            )
        }
        val treatment = treatments.single()

        val freight = consignments.sumOf { it.freight_paise }
        val gst = consignments.sumOf { it.gst_paise }
        val total = consignments.sumOf { it.total_paise }
        val taxable = consignments.sumOf { c ->
            database.consignmentDao().getChargeLines(c.local_id).filter { it.taxable }.sumOf { it.computed_paise }
        }

        val bill = FreightBillEntity(
            local_id = "fb-" + UUID.randomUUID().toString(),
            server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.PENDING, deleted_at = null,
            company_id = session.companyId,
            series_id = database.numberingDao().getSeries(session.companyId, session.branchId, "FREIGHT_BILL")?.local_id ?: "",
            bill_no = null, state = "DRAFT",
            party_id = partyId,
            period_start = consignments.minOf { it.booked_at }, period_end = consignments.maxOf { it.booked_at },
            due_at = dueAt,
            freight_paise = freight, other_charges_paise = total - freight - gst,
            taxable_paise = taxable, gst_paise = gst, total_paise = total,
            gst_treatment = treatment, notes = notes,
            issued_at = null, issued_by_name = null, cancelled_at = null,
        )
        database.billingDao().upsertBill(bill)
        consignments.forEach { c ->
            database.consignmentDao().upsertConsignment(c.copy(freight_bill_id = bill.local_id, updated_at_local = now))
            outboxWriter.enqueue(
                op = OutboxOp.UPDATE,
                entityType = OutboxEntityType.CONSIGNMENT,
                entityLocalId = c.local_id,
                payloadJson = JSONObject().put("freight_bill_id", bill.local_id).toString(),
                now = now,
            )
        }
        outboxWriter.enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.FREIGHT_BILL,
            entityLocalId = bill.local_id,
            payloadJson = billPayload(bill),
            now = now,
        )
        val party = database.billingDao().getPartyNameGstin(partyId)
        Result.success(bill.asHeader(party?.name ?: "", party?.gstin))
    }

    override suspend fun removeConsignmentFromDraft(billId: String, consignmentId: String, now: Long): Result<Unit> = database.withTransaction {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        val bill = database.billingDao().getBill(billId)
            ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No such bill")
        if (bill.state != "DRAFT") {
            return@withTransaction Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "An issued bill is immutable — corrections go on a credit note (§12.1)")
        }
        val consignment = database.consignmentDao().getConsignment(consignmentId)
            ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No such consignment")
        if (consignment.freight_bill_id != billId) {
            return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "That consignment is not on this bill")
        }
        database.consignmentDao().upsertConsignment(consignment.copy(freight_bill_id = null, updated_at_local = now))
        recomputeTotals(bill, now)
        outboxWriter.enqueue(
            op = OutboxOp.UPDATE,
            entityType = OutboxEntityType.CONSIGNMENT,
            entityLocalId = consignmentId,
            payloadJson = JSONObject().put("freight_bill_id", JSONObject.NULL).toString(),
            now = now,
        )
        Result.success(Unit)
    }

    override suspend fun cancelBill(billId: String, now: Long): Result<Unit> = database.withTransaction {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        val bill = database.billingDao().getBill(billId)
            ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No such bill")
        if (bill.state == "CANCELLED") {
            return@withTransaction Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "This bill is already cancelled")
        }
        // §12.1: the number is retained and the consignments return to the pool.
        database.billingDao().getBillConsignments(billId).forEach { row ->
            database.consignmentDao().getConsignment(row.local_id)?.let { c ->
                if (c.freight_bill_id == billId) {
                    database.consignmentDao().upsertConsignment(c.copy(freight_bill_id = null, updated_at_local = now))
                    outboxWriter.enqueue(
                        op = OutboxOp.UPDATE,
                        entityType = OutboxEntityType.CONSIGNMENT,
                        entityLocalId = c.local_id,
                        payloadJson = JSONObject().put("freight_bill_id", JSONObject.NULL).toString(),
                        now = now,
                    )
                }
            }
        }
        database.billingDao().upsertBill(bill.copy(state = "CANCELLED", cancelled_at = now, updated_at_local = now))
        outboxWriter.enqueue(
            op = OutboxOp.UPDATE,
            entityType = OutboxEntityType.FREIGHT_BILL,
            entityLocalId = billId,
            payloadJson = JSONObject().put("state", "CANCELLED").put("cancelled_at", now).toString(),
            now = now,
        )
        Result.success(Unit)
    }

    override suspend fun issueBill(billId: String, now: Long): Result<Unit> {
        val bill = database.billingDao().getBill(billId)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No such bill")
        if (bill.state == "ISSUED") {
            return Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "This bill is already issued")
        }
        if (bill.state == "CANCELLED") {
            return Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "A cancelled bill cannot be issued")
        }
        // Phase 2 is draft-only for issuing (Phase2.md §1): one-consignment-one-live-bill is
        // a server guarantee, so the draft is saved and the number is never used twice.
        return Result.failure(
            ErrorCode.OFFLINE_UNAVAILABLE,
            "A freight bill can only be issued online, so the number is never used twice. You're offline — the draft is saved.",
        )
    }
    override fun observeBill(billId: String): Flow<BillDetail?> =
        combine(
            database.billingDao().observeBillWithParty(billId),
            database.billingDao().observeBillConsignments(billId),
        ) { bill, rows ->
            bill?.let { b ->
                val allocated = database.billingDao().sumBillAllocated(billId)
                BillDetail(
                    bill = BillHeader(
                        localId = b.local_id, billNo = b.bill_no, state = b.state,
                        partyId = b.party_id, partyName = b.party_name, partyGstin = b.party_gstin,
                        periodStart = b.period_start, periodEnd = b.period_end, dueAt = b.due_at,
                        freightPaise = b.freight_paise, otherChargesPaise = b.other_charges_paise,
                        taxablePaise = b.taxable_paise, gstPaise = b.gst_paise, totalPaise = b.total_paise,
                        gstTreatment = b.gst_treatment, notes = b.notes,
                        issuedAt = b.issued_at, issuedByName = b.issued_by_name, cancelledAt = b.cancelled_at,
                    ),
                    rows = rows.map { it.toLine() },
                    outstandingPaise = b.total_paise - allocated,
                )
            }
        }

    override fun observeTopayAwaiting(): Flow<List<TopayLine>> = flow {
        val s = sessionRepository.session.first()
        emitAll(
            database.billingDao().observeTopayAwaiting(s.companyId, s.branchId)
                .map { rows ->
                    rows.map { r ->
                        TopayLine(r.local_id, r.display_no, r.consignee_id, r.consignee_name, r.total_paise, r.status, r.held_remark, r.waived)
                    }
                },
        )
    }

    override fun observeRecentReceipts(): Flow<List<ReceiptLine>> = flow {
        val s = sessionRepository.session.first()
        emitAll(
            database.billingDao().observeReceipts(s.companyId, s.branchId, null)
                .map { rows ->
                    rows.map { r ->
                        ReceiptLine(r.local_id, r.receipt_no, r.party_name, r.instrument, r.instrument_ref, r.received_at, r.amount_paise)
                    }
                },
        )
    }

    override suspend fun receiptsSinceSummary(sinceAt: Long): Pair<Long, Int> {
        val s = sessionRepository.session.first()
        val dao = database.billingDao()
        return dao.sumReceiptsSince(s.companyId, s.branchId, sinceAt) to dao.countReceiptsSince(s.companyId, s.branchId, sinceAt)
    }

    override suspend fun recordReceipt(payerPartyId: String, amountPaise: Long, instrument: String, instrumentRef: String?, allocations: List<AllocationInput>, now: Long): Result<ReceiptSaved> = database.withTransaction {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        if (amountPaise <= 0) return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "A receipt needs an amount")
        val allocated = allocations.sumOf { it.amountPaise }
        if (allocated > amountPaise) {
            return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "The allocation exceeds the receipt")
        }
        if (instrument != "CASH" && instrumentRef.isNullOrBlank()) {
            return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "A non-cash receipt needs its reference")
        }
        allocations.forEach { a ->
            when (a.targetType) {
                "BILL" -> {
                    val bill = a.billId?.let { database.billingDao().getBill(it) }
                        ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No such bill to allocate to")
                    if (bill.state != "ISSUED") return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Receipts allocate to issued bills only")
                    val outstanding = bill.total_paise - database.billingDao().sumBillAllocated(bill.local_id)
                    if (a.amountPaise > outstanding) {
                        return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "The allocation exceeds the bill's outstanding amount")
                    }
                }
                "TOPAY_CONSIGNMENT" -> {
                    val c = a.consignmentId?.let { database.consignmentDao().getConsignment(it) }
                        ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No such consignment to collect against")
                    if (a.amountPaise > c.total_paise) {
                        return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "The collection exceeds the consignment's To Pay amount")
                    }
                }
                "ON_ACCOUNT" -> Unit
                else -> return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "Unknown allocation target")
            }
        }

        val stamped = numberingRepository.issueNext(session.companyId, session.branchId, "RECEIPT", now)
        val number = (stamped as? Result.Success)?.value?.display
        val receipt = ReceiptEntity(
            local_id = "rcpt-" + UUID.randomUUID().toString(),
            server_id = null, updated_at_local = now, updated_at_server = null,
            sync_state = SyncState.PENDING, deleted_at = null,
            company_id = session.companyId,
            series_id = database.numberingDao().getSeries(session.companyId, session.branchId, "RECEIPT")?.local_id ?: "",
            receipt_no = number,
            party_id = payerPartyId, amount_paise = amountPaise,
            instrument = instrument, instrument_ref = instrumentRef?.trim(),
            received_at = now, received_at_branch_id = session.branchId,
            received_by_name = session.name, notes = null,
        )
        database.billingDao().upsertReceipt(receipt)
        allocations.forEach { a ->
            val allocation = ReceiptAllocationEntity(
                local_id = "alloc-" + UUID.randomUUID().toString(),
                server_id = null, updated_at_local = now, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null,
                company_id = session.companyId,
                receipt_id = receipt.local_id,
                target_type = a.targetType, bill_id = a.billId, consignment_id = a.consignmentId,
                amount_paise = a.amountPaise,
            )
            database.billingDao().upsertAllocation(allocation)
            outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.RECEIPT_ALLOCATION,
                entityLocalId = allocation.local_id,
                payloadJson = JSONObject()
                    .put("receipt_id", receipt.local_id)
                    .put("target_type", a.targetType)
                    .put("bill_id", a.billId ?: JSONObject.NULL)
                    .put("consignment_id", a.consignmentId ?: JSONObject.NULL)
                    .put("amount_paise", a.amountPaise)
                    .toString(),
                now = now,
            )
        }
        outboxWriter.enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.RECEIPT,
            entityLocalId = receipt.local_id,
            payloadJson = JSONObject()
                .put("receipt_no", number ?: JSONObject.NULL)
                .put("party_id", payerPartyId)
                .put("amount_paise", amountPaise)
                .put("instrument", instrument)
                .put("instrument_ref", instrumentRef ?: JSONObject.NULL)
                .put("received_at", now)
                .put("received_at_branch_id", session.branchId)
                .put("received_by_name", session.name)
                .toString(),
            now = now,
        )
        Result.success(ReceiptSaved(receipt.local_id, receipt.receipt_no, receipt.amount_paise))
    }

    override suspend fun outstandingBillsForParty(partyId: String): List<OutstandingBill> {
        val s = sessionRepository.session.first()
        return database.billingDao().getOutstandingBills(s.companyId, partyId).map { row ->
            OutstandingBill(
                localId = row.local_id, billNo = row.bill_no, totalPaise = row.total_paise,
                issuedAt = row.issued_at, dueAt = row.due_at,
                outstandingPaise = row.total_paise - row.allocated_paise,
            )
        }.filter { it.outstandingPaise > 0 }
    }

    override suspend fun partiesWithIssuedBills(): List<PartyChoice> {
        val s = sessionRepository.session.first()
        return database.billingDao().getPartiesWithIssuedBills(s.companyId).map { PartyChoice(it.party_id, it.party_name) }
    }

    override suspend fun statement(partyId: String, periodStart: Long, periodEnd: Long, now: Long): Result<Statement> {
        val s = sessionRepository.session.first()
        val dao = database.billingDao()
        val party = dao.getPartyNameGstin(partyId)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No such party")

        val bills = dao.getIssuedBillsForParty(s.companyId, partyId)
        val receipts = dao.getReceiptsForParty(s.companyId, partyId)
        val creditNotes = dao.getCreditNotesForParty(s.companyId, partyId)
        val billAllocations = if (bills.isNotEmpty()) dao.getAllocationsForBills(bills.map { it.local_id }) else emptyList()
        val receiptAllocations = if (receipts.isNotEmpty()) dao.getAllocationsForReceipts(receipts.map { it.local_id }) else emptyList()

        // Opening: everything before the period. Bills debit; receipts and credit notes credit.
        val opening = bills.filter { (it.issued_at ?: it.updated_at_local) < periodStart }.sumOf { it.total_paise } -
            receipts.filter { it.received_at < periodStart }.sumOf { it.amount_paise } -
            creditNotes.filter { it.created_at < periodStart }.sumOf { it.amount_paise }

        data class Entry(val at: Long, val docNo: String, val description: String, val debit: Long, val credit: Long)
        val entries = buildList {
            bills.filter { inPeriod(it.issued_at ?: it.updated_at_local, periodStart, periodEnd) }.forEach { b ->
                add(Entry(b.issued_at ?: b.updated_at_local, b.bill_no ?: "(no number)", "Freight bill", b.total_paise, 0))
            }
            receipts.filter { inPeriod(it.received_at, periodStart, periodEnd) }.forEach { r ->
                val targets = receiptAllocations.filter { it.receipt_id == r.local_id }
                val description = when {
                    targets.isEmpty() -> "${r.instrument} · on account"
                    else -> "${r.instrument} · " + targets.joinToString(", ") { t ->
                        when (t.target_type) {
                            "BILL" -> "applied to bill"
                            "TOPAY_CONSIGNMENT" -> "To Pay collection"
                            else -> "on account"
                        }
                    }
                }
                add(Entry(r.received_at, r.receipt_no ?: "(no number)", description, 0, r.amount_paise))
            }
            creditNotes.filter { inPeriod(it.created_at, periodStart, periodEnd) }.forEach { cn ->
                add(Entry(cn.created_at, cn.note_no ?: "(no number)", "Credit note · ${cn.reason}", 0, cn.amount_paise))
            }
        }.sortedBy { it.at }

        var balance = opening
        val rows = entries.map { e ->
            balance += e.debit - e.credit
            StatementRow(at = e.at, docNo = e.docNo, description = e.description, debitPaise = e.debit, creditPaise = e.credit, balancePaise = balance)
        }
        val closing = balance

        // Ageing: each issued bill's outstanding portion, bucketed by its issue date.
        val over90 = bills.sumOf { b ->
            val outstanding = b.total_paise - billAllocations.filter { it.bill_id == b.local_id }.sumOf { it.amount_paise }
            val ageDays = (now - (b.issued_at ?: b.updated_at_local)) / DAY_MS
            if (outstanding > 0 && ageDays > 90) outstanding else 0L
        }

        return Result.success(
            Statement(
                partyId = partyId, partyName = party.name, gstin = party.gstin,
                periodStart = periodStart, periodEnd = periodEnd,
                openingPaise = opening, rows = rows, closingPaise = closing, over90Paise = over90,
            ),
        )
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private suspend fun branchId(filter: PoolFilter): String? =
        if (filter.allBranches) null else sessionRepository.session.first().branchId

    /** (sinceAt, agedBefore) from the chip state; quarter = the calendar quarter of today. */
    private fun periodBounds(filter: PoolFilter): Pair<Long?, Long?> {
        val now = System.currentTimeMillis()
        val since = if (filter.thisQuarterOnly) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            cal.set(Calendar.MONTH, (cal.get(Calendar.MONTH) / 3) * 3)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } else null
        val agedBefore = filter.minAgeDays?.let { now - it * DAY_MS }
        return since to agedBefore
    }

    private fun inPeriod(at: Long, start: Long, end: Long): Boolean = at in start..end

    private suspend fun recomputeTotals(bill: FreightBillEntity, now: Long) {
        val consignments = database.billingDao().getBillConsignments(bill.local_id)
            .mapNotNull { database.consignmentDao().getConsignment(it.local_id) }
        val freight = consignments.sumOf { it.freight_paise }
        val gst = consignments.sumOf { it.gst_paise }
        val total = consignments.sumOf { it.total_paise }
        val taxable = consignments.sumOf { c ->
            database.consignmentDao().getChargeLines(c.local_id).filter { it.taxable }.sumOf { it.computed_paise }
        }
        database.billingDao().upsertBill(
            bill.copy(
                freight_paise = freight, gst_paise = gst, total_paise = total,
                other_charges_paise = total - freight - gst, taxable_paise = taxable,
                period_start = consignments.minOfOrNull { it.booked_at } ?: bill.period_start,
                period_end = consignments.maxOfOrNull { it.booked_at } ?: bill.period_end,
                updated_at_local = now,
            ),
        )
    }

    private fun FreightBillEntity.asHeader(partyName: String, partyGstin: String?) = BillHeader(
        localId = local_id, billNo = bill_no, state = state,
        partyId = party_id, partyName = partyName, partyGstin = partyGstin,
        periodStart = period_start, periodEnd = period_end, dueAt = due_at,
        freightPaise = freight_paise, otherChargesPaise = other_charges_paise,
        taxablePaise = taxable_paise, gstPaise = gst_paise, totalPaise = total_paise,
        gstTreatment = gst_treatment, notes = notes,
        issuedAt = issued_at, issuedByName = issued_by_name, cancelledAt = cancelled_at,
    )

    private fun billPayload(bill: FreightBillEntity): String = JSONObject()
        .put("bill_no", bill.bill_no ?: JSONObject.NULL)
        .put("state", bill.state)
        .put("party_id", bill.party_id)
        .put("period_start", bill.period_start)
        .put("period_end", bill.period_end)
        .put("due_at", bill.due_at ?: JSONObject.NULL)
        .put("freight_paise", bill.freight_paise)
        .put("gst_paise", bill.gst_paise)
        .put("total_paise", bill.total_paise)
        .put("gst_treatment", bill.gst_treatment)
        .put("notes", bill.notes ?: JSONObject.NULL)
        .toString()

    companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
