package com.example.transportapp.data.transport.consignment

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.dao.ConsignmentDao
import com.example.transportapp.core.database.dao.MastersDao
import com.example.transportapp.core.database.entity.ChargeLineEntity
import com.example.transportapp.core.database.entity.ConsignmentEntity
import com.example.transportapp.core.database.entity.ConsignmentItemEntity
import com.example.transportapp.core.database.entity.DocSnapshotEntity
import com.example.transportapp.core.database.entity.StatusEventEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.rate.BookingCalcSettings
import com.example.transportapp.data.transport.rate.RateCardRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
import com.example.transportapp.domain.transport.consignment.ConsignmentStateMachine
import com.example.transportapp.data.transport.tracking.NewStatusEvent
import com.example.transportapp.domain.transport.calc.CalculationInput
import com.example.transportapp.domain.transport.calc.CalculationResult
import com.example.transportapp.domain.transport.calc.ChargeCalculator
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One article row of a multi-article booking (S15): per-item goods and weight. */
data class BookingItem(
    val goodsId: String?,
    val description: String,
    val packages: Long,
    val actualWeightG: Long,
)

/** The computed booking the T5 form hands over when the clerk taps "Book and print" (§3). */
data class BookingDraft(
    val consignorId: String,
    val consigneeId: String,
    val routeId: String,
    val goodsId: String?,
    val goodsDescription: String,
    val paymentMode: PaymentMode,
    val risk: String,
    val deliveryType: String,
    val packages: Long,
    val actualWeightG: Long,
    val declaredValuePaise: Long,
    val ewayBillNo: String?,
    val privateMark: String?,
    /**
     * The form's §10.4 inputs. book() recomputes with the freshly resolved rate - a stale
     * form can never print (§8).
     */
    val calculationInput: CalculationInput,
    /** Articles beyond the first (S15 multi-article); empty for the single-article case. */
    val extraItems: List<BookingItem> = emptyList(),
)

/** What T5 prefills from when raising a §16.1 amendment (S15). */
data class AmendmentPrefill(
    val consignorId: String,
    val consigneeId: String,
    val routeId: String,
    val goodsId: String?,
    val goodsDescription: String,
    val paymentMode: PaymentMode,
    val packages: Long,
    val actualWeightG: Long,
)

/** What booking produced: the stamped number, the case-file id. */
data class BookingResult(
    val consignmentLocalId: String,
    val biltyNo: String,
    val provisional: Boolean,
)

/** The T6 read model — the latest immutable print payload plus its metadata (§8). */
data class BiltySnapshot(
    val consignmentLocalId: String,
    val biltyNo: String,
    val templateVersion: String,
    val version: Int,
    val copyCount: Int,
    val contentHash: String,
    val payload: BiltySnapshotPayload,
)

/**
 * The §8 print payload — every value that prints, as print-ready strings. Superseding a
 * snapshot (provisional renumbering, §9) appends a new DOC_SNAPSHOT_E row; this shape is
 * the version-1 bilty template's binding.
 */
data class BiltySnapshotPayload(
    val companyName: String,
    val addressLine: String,
    val contactLine: String,
    val consignorName: String,
    val consignorContact: String,
    val consignorGstin: String,
    val consignorAddress: String,
    val consigneeName: String,
    val consigneeContact: String,
    val consigneeGstin: String,
    val consigneeAddress: String,
    val docNo: String,
    val date: String,
    val fromStation: String,
    val toStation: String,
    val packages: String,
    val goodsDescription: String,
    val actualWeight: String,
    val chargeableWeight: String,
    val rate: String,
    val freight: String,
    val hamali: String,
    val doorDelivery: String,
    val taxable: String,
    val gst: String,
    val rounding: String,
    val gstLabel: String,
    val totalLabel: String,
    val grandTotal: String,
    val amountInWords: String,
    val stamp: String,
    val footer: String,
    /** Printed as a cross-reference once a provisional number is renumbered (§9). */
    val provisionalCrossRef: String? = null,
)

/**
 * The consignment aggregate (Phase2.md S5). `book` is the transactional heart: numbering,
 * the recomputed §10.4 charges, the consignment + item + charge lines + Booked event + the
 * first document snapshot, and the outbox rows all commit or nothing does (§3.4 #5, #8).
 *
 * S15 adds the §7.1 lifecycle tails: `amend` books a successor consignment linked by
 * amends_id with its reason carried on the amendment row (§16.1), and `cancel` moves a
 * Booked consignment to Cancelled behind a Manager gate with a §7.2-strength reason — the
 * number is retained forever and never reused (§7.1).
 */
interface ConsignmentRepository {

    suspend fun book(draft: BookingDraft): Result<BookingResult>

    /** §16.1: the amendment is a fresh consignment that supersedes [originalLocalId]. */
    suspend fun amend(originalLocalId: String, reason: String, draft: BookingDraft): Result<BookingResult>

    /** Loads the original's scope and quantities so T5 can prefill the amendment (S15). */
    suspend fun loadForAmendment(companyId: String, biltyNo: String): AmendmentPrefill?

    /** §7.1: Manager-gated cancel with a ≥10-character reason; the number is retained. */
    suspend fun cancel(biltyNo: String, reason: String): Result<Unit>

    suspend fun snapshotByBiltyNo(companyId: String, biltyNo: String): BiltySnapshot?
}

@Singleton
class ConsignmentRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val consignmentDao: ConsignmentDao,
    private val mastersDao: MastersDao,
    private val sessionRepository: SessionRepository,
    private val rateCardRepository: RateCardRepository,
    private val numberingRepository: com.example.transportapp.data.transport.numbering.NumberingRepository,
    private val outboxWriter: OutboxWriter,
    private val statusRepository: com.example.transportapp.data.transport.tracking.StatusRepository,
) : ConsignmentRepository {

    /** Test seam: throws before the snapshot insert so the transaction's rollback is provable. */
    @Volatile
    var debugFailBeforeSnapshot: Boolean = false

    override suspend fun book(draft: BookingDraft): Result<BookingResult> =
        bookInternal(draft, amendsId = null, amendmentReason = null)

    override suspend fun amend(originalBiltyNo: String, reason: String, draft: BookingDraft): Result<BookingResult> {
        val session = sessionRepository.session.first()
        if (session.role != "OWNER" && session.role != "MANAGER") {
            return Result.failure(ErrorCode.AUTH_NO_ACCESS, "Amending an issued bilty is a Manager's call — ask the office")
        }
        if (reason.trim().length < 10) {
            return Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "An amendment needs a reason of at least ten characters")
        }
        val original = consignmentDao.getConsignmentByBiltyNo(session.companyId, originalBiltyNo)
            ?: consignmentDao.getConsignmentByProvisionalNo(session.companyId, originalBiltyNo)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No bilty $originalBiltyNo on this device")
        return bookInternal(draft, amendsId = original.local_id, amendmentReason = reason.trim())
    }

    override suspend fun loadForAmendment(companyId: String, biltyNo: String): AmendmentPrefill? {
        val consignment = consignmentDao.getConsignmentByBiltyNo(companyId, biltyNo)
            ?: consignmentDao.getConsignmentByProvisionalNo(companyId, biltyNo)
            ?: return null
        val items = consignmentDao.getItems(consignment.local_id)
        return AmendmentPrefill(
            consignorId = consignment.consignor_id,
            consigneeId = consignment.consignee_id,
            routeId = consignment.route_id,
            goodsId = items.firstOrNull()?.goods_id,
            goodsDescription = items.firstOrNull()?.description ?: "",
            paymentMode = runCatching { PaymentMode.valueOf(consignment.payment_mode) }.getOrDefault(PaymentMode.TOPAY),
            packages = consignment.packages,
            actualWeightG = consignment.actual_weight_g,
        )
    }

    override suspend fun cancel(biltyNo: String, reason: String): Result<Unit> {
        val session = sessionRepository.session.first()
        if (session.role != "OWNER" && session.role != "MANAGER") {
            return Result.failure(ErrorCode.AUTH_NO_ACCESS, "Cancelling a bilty is a Manager's call — ask the office")
        }
        if (reason.trim().length < 10) {
            return Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "A cancellation needs a reason of at least ten characters")
        }
        val consignment = consignmentDao.getConsignmentByBiltyNo(session.companyId, biltyNo)
            ?: consignmentDao.getConsignmentByProvisionalNo(session.companyId, biltyNo)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No bilty $biltyNo on this device")
        val from = runCatching { ConsignmentStatus.valueOf(consignment.status_projection) }.getOrDefault(ConsignmentStatus.BOOKED)
        if (!ConsignmentStateMachine.canTransition(from, ConsignmentStatus.CANCELLED)) {
            return Result.failure(
                ErrorCode.CONSIGNMENT_IMMUTABLE,
                "A bilty in status ${from.wording} cannot be cancelled — only a Booked one can",
            )
        }
        // The CANCELLED event rides the append-only log: the projection advances with it,
        // the outbox carries it, and the bilty number is retained forever (§7.1).
        val event = NewStatusEvent(
            biltyNo = biltyNo,
            eventType = "CANCELLED",
            remark = reason.trim(),
            reasonCode = "MANAGER_CANCEL",
        )
        return when (val result = statusRepository.append(event, System.currentTimeMillis())) {
            is com.example.transportapp.core.common.Result.Success -> Result.success(Unit)
            is com.example.transportapp.core.common.Result.Failure -> Result.failure(result.code, result.message)
        }
    }

    private suspend fun bookInternal(
        draft: BookingDraft,
        amendsId: String?,
        amendmentReason: String?,
    ): Result<BookingResult> {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        val companyId = session.companyId
        val now = System.currentTimeMillis()

        val resolvedRate = rateCardRepository.resolveBookingRate(
            companyId,
            partyId = draft.consignorId,
            routeId = draft.routeId,
            goodsId = draft.goodsId,
        )
        val settings = rateCardRepository.bookingSettings(companyId, draft.routeId)
        // S15 multi-article: the rate walk prices the aggregate — Σ packages, Σ weight.
        val calcInput = draft.calculationInput.copy(
            rate = resolvedRate,
            packages = draft.packages + draft.extraItems.sumOf { it.packages },
            actualWeightG = draft.actualWeightG + draft.extraItems.sumOf { it.actualWeightG },
        )
        val calculation = ChargeCalculator.calculate(calcInput)

        val series = database.numberingDao().getSeries(companyId, session.branchId, "BILTY")
            ?: return Result.failure(ErrorCode.LEASE_INVALID, "No bilty series for this branch")
        val consignor = mastersDao.getParty(draft.consignorId)
        val consignee = mastersDao.getParty(draft.consigneeId)
        val route = mastersDao.getRoute(draft.routeId)
        val fromStation = route?.let { mastersDao.getStation(it.origin_station_id) }
        val toStation = route?.let { mastersDao.getStation(it.dest_station_id) }
        val expectedArrival = now + (route?.transit_days?.toLong() ?: 2L) * 24 * 60 * 60 * 1000

        return database.withTransaction {
            // 1. The number is stamped at issue time, inside the same transaction (§9).
            val issued = when (val number = numberingRepository.issueNext(companyId, session.branchId, "BILTY", now)) {
                is com.example.transportapp.core.common.Result.Success -> number.value
                is com.example.transportapp.core.common.Result.Failure ->
                    return@withTransaction Result.failure(number.code, number.message)
            }
            val consignmentId = "cn-" + UUID.randomUUID().toString()

            // 2. The consignment row; the projection matches the event written below (§7.1, D1).
            val consignment = ConsignmentEntity(
                local_id = consignmentId,
                server_id = null,
                updated_at_local = now,
                updated_at_server = null,
                sync_state = SyncState.PENDING,
                deleted_at = null,
                company_id = companyId,
                series_id = series.local_id,
                bilty_no = if (issued.provisional) null else issued.display,
                provisional_no = if (issued.provisional) issued.display else null,
                status_projection = "BOOKED",
                booking_branch_id = session.branchId,
                dest_branch_id = null,
                consignor_id = draft.consignorId,
                consignee_id = draft.consigneeId,
                route_id = draft.routeId,
                from_station_id = route?.origin_station_id ?: draft.routeId,
                to_station_id = route?.dest_station_id ?: draft.routeId,
                payment_mode = draft.paymentMode.name,
                risk = draft.risk,
                delivery_type = draft.deliveryType,
                place_of_supply_state = settings.defaultPlaceOfSupplyState,
                eway_bill_no = draft.ewayBillNo,
                private_mark = draft.privateMark,
                packages = calcInput.packages,
                actual_weight_g = calcInput.actualWeightG,
                chargeable_weight_g = calculation.chargeableWeightG,
                declared_value_paise = draft.declaredValuePaise,
                freight_paise = calculation.freightPaise ?: 0L,
                gst_paise = calculation.gst?.totalPaise ?: 0L,
                total_paise = calculation.grandTotalPaise,
                booked_at = now,
                booked_by_name = session.name,
                expected_arrival = expectedArrival,
                party_names = "${consignor?.name.orEmpty()}; ${consignee?.name.orEmpty()}",
                freight_bill_id = null,
                amends_id = amendsId,
                amendment_reason = amendmentReason,
            )

            // 3. Goods rows (one per article, S15) and charge lines, frozen at booking (§16.1).
            //    The rate walk priced the aggregate; every article row carries the consignment's
            //    chargeable weight so the register math reconciles either way.
            val allItems = listOf(BookingItem(draft.goodsId, draft.goodsDescription, draft.packages, draft.actualWeightG)) + draft.extraItems
            val items = allItems.map { article ->
                ConsignmentItemEntity(
                    local_id = "cni-" + UUID.randomUUID().toString(),
                    server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null,
                    consignment_id = consignmentId,
                    goods_id = article.goodsId,
                    description = article.description,
                    packages = article.packages,
                    actual_weight_g = article.actualWeightG,
                    chargeable_weight_g = calculation.chargeableWeightG,
                    rate_paise = resolvedRate?.candidate?.ratePaise,
                    basis = resolvedRate?.candidate?.basis?.name,
                    freight_paise = calculation.freightPaise ?: 0L,
                )
            }
            val chargeLines = calculation.lines.mapIndexed { index, line ->
                ChargeLineEntity(
                    local_id = "cl-" + UUID.randomUUID().toString(),
                    server_id = null, updated_at_local = now, updated_at_server = null,
                    sync_state = SyncState.PENDING, deleted_at = null,
                    consignment_id = consignmentId,
                    head_code = line.headCode ?: line.label.lowercase(),
                    label = line.label,
                    basis = line.detail,
                    input_value = line.detail,
                    computed_paise = line.amountPaise,
                    taxable = line.taxable,
                    sort_order = index,
                )
            }

            // 4. The first status event — append-only (§7.2).
            val event = StatusEventEntity(
                local_id = "ev-" + UUID.randomUUID().toString(),
                server_id = null, updated_at_local = now, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null,
                company_id = companyId,
                consignment_id = consignmentId,
                client_event_id = UUID.randomUUID().toString(),
                event_type = "BOOKED",
                occurred_at = now,
                recorded_at = now,
                actor_member_id = session.userId,
                actor_name = session.name,
                branch_id = session.branchId,
                location = null,
                photo_ref = null,
                reason_code = null,
                remark = null,
                challan_ref = null,
            )

            // 5. The first document snapshot — the print payload is frozen here (§8).
            if (debugFailBeforeSnapshot) {
                throw IllegalStateException("debugFailBeforeSnapshot armed")
            }
            val snapshotPayload = buildSnapshotPayload(
                calculation = calculation,
                draft = draft,
                issued = issued,
                consignor = consignor,
                consignee = consignee,
                fromStationName = fromStation?.name ?: "INDORE",
                toStationName = toStation?.name ?: "NASHIK",
                bookedAt = now,
            )
            val payloadJson = encodePayload(snapshotPayload)
            val snapshot = DocSnapshotEntity(
                local_id = "snap-" + UUID.randomUUID().toString(),
                server_id = null, updated_at_local = now, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null,
                consignment_id = consignmentId,
                document_type = "BILTY",
                template_id = "tpl-bilty-default",
                template_version = "1",
                version = 1,
                payload_json = payloadJson,
                content_hash = fnv1a(payloadJson).toString(16),
                copy_count = 4,
            )

            consignmentDao.upsertConsignment(consignment)
            items.forEach { consignmentDao.upsertItem(it) }
            chargeLines.forEach { consignmentDao.upsertChargeLine(it) }
            consignmentDao.insertStatusEvent(event)
            consignmentDao.upsertSnapshot(snapshot)

            // 6. Outbox rows commit atomically with the entities (§3.4 #5); the snapshot's
            //    row declares the consignment's op as its prerequisite (§16.2).
            val consignmentOp = outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.CONSIGNMENT,
                entityLocalId = consignmentId,
                payloadJson = ConsignmentPayload.from(consignment).toJson(),
                now = now,
            )
            outboxWriter.enqueue(
                op = OutboxOp.INSERT,
                entityType = OutboxEntityType.DOC_SNAPSHOT,
                entityLocalId = snapshot.local_id,
                payloadJson = SnapshotPayload.from(snapshot).toJson(),
                prerequisites = listOf(consignmentOp),
                now = now,
            )

            Result.success(
                BookingResult(
                    consignmentLocalId = consignmentId,
                    biltyNo = issued.display,
                    provisional = issued.provisional,
                ),
            )
        }
    }

    override suspend fun snapshotByBiltyNo(companyId: String, biltyNo: String): BiltySnapshot? {
        val consignment = consignmentDao.getConsignmentByBiltyNo(companyId, biltyNo)
            ?: consignmentDao.getConsignmentByProvisionalNo(companyId, biltyNo)
            ?: return null
        val snapshot = consignmentDao.getLatestSnapshot(consignment.local_id) ?: return null
        val payload = decodePayload(snapshot.payload_json) ?: return null
        return BiltySnapshot(
            consignmentLocalId = consignment.local_id,
            biltyNo = consignment.bilty_no ?: consignment.provisional_no ?: biltyNo,
            templateVersion = snapshot.template_version,
            version = snapshot.version,
            copyCount = snapshot.copy_count,
            contentHash = snapshot.content_hash,
            payload = payload,
        )
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private fun buildSnapshotPayload(
        calculation: CalculationResult,
        draft: BookingDraft,
        issued: com.example.transportapp.data.transport.numbering.IssuedNumber,
        consignor: com.example.transportapp.core.database.entity.PartyEntity?,
        consignee: com.example.transportapp.core.database.entity.PartyEntity?,
        fromStationName: String,
        toStationName: String,
        bookedAt: Long,
    ): BiltySnapshotPayload = BiltySnapshotPayload(
        companyName = "SHIVSHAKTI ROADLINES",
        addressLine = "Plot 14, Transport Nagar, Indore 452003",
        contactLine = "Ph 94250 61183 · GSTIN 23AABCS4521M1Z9",
        consignorName = consignor?.name.orEmpty(),
        consignorContact = listOfNotNull(consignor?.station, consignor?.phone).joinToString(" · "),
        consignorGstin = consignor?.gstin?.let { "GSTIN $it" } ?: "",
        consignorAddress = listOfNotNull(consignor?.street_address, consignor?.station, consignor?.pincode).joinToString(", "),
        consigneeName = consignee?.name.orEmpty(),
        consigneeContact = listOfNotNull(consignee?.station, consignee?.phone).joinToString(" · "),
        consigneeGstin = consignee?.gstin?.let { "GSTIN $it" } ?: "",
        consigneeAddress = listOfNotNull(consignee?.street_address, consignee?.station, consignee?.pincode).joinToString(", "),
        docNo = issued.display,
        date = formatDate(bookedAt),
        fromStation = fromStationName.uppercase(),
        toStation = toStationName.uppercase(),
        packages = draft.packages.toString(),
        goodsDescription = draft.goodsDescription.uppercase(),
        actualWeight = (draft.actualWeightG / 1000).toString() + " kg",
        chargeableWeight = (calculation.chargeableWeightG / 1000).toString() + " kg",
        rate = resolvedRateText(draft),
        freight = Money(calculation.freightPaise ?: 0L).formatted(),
        hamali = chargeOf(calculation, "hamali"),
        doorDelivery = chargeOf(calculation, "door_delivery"),
        taxable = Money(calculation.taxablePaise).formatted(),
        gst = Money(calculation.gst?.totalPaise ?: 0L).formatted(),
        rounding = Money(kotlin.math.abs(calculation.roundingDeltaPaise)).formatted(),
        gstLabel = calculation.lines.firstOrNull { it.headCode == "gst" }?.label ?: "GST",
        totalLabel = "Grand Total",
        grandTotal = Money(calculation.grandTotalPaise).formatted(),
        amountInWords = Money(calculation.grandTotalPaise).inWordsLedger(),
        stamp = stampOf(draft.paymentMode),
        footer = footerOf(draft),
        provisionalCrossRef = null,
    )

    private fun resolvedRateText(draft: BookingDraft): String {
        val candidate = draft.calculationInput.rate?.candidate ?: return "—"
        return Money(candidate.ratePaise).formatted()
    }

    private fun chargeOf(calculation: CalculationResult, code: String): String {
        val line = calculation.lines.firstOrNull { it.headCode == code } ?: return "0.00"
        return Money(line.amountPaise).formatted()
    }

    private fun stampOf(mode: PaymentMode): String = when (mode) {
        PaymentMode.PAID -> "PAID"
        PaymentMode.TOPAY -> "TO PAY"
        PaymentMode.TBB -> "TBB"
    }

    private fun footerOf(draft: BookingDraft): String {
        val risk = if (draft.risk == "OWNERS") "At owner's risk" else "At carrier's risk"
        val delivery = if (draft.deliveryType == "DOOR") "Door delivery" else "Godown delivery"
        return listOf(
            risk,
            delivery,
            draft.privateMark?.let { "Private mark $it" },
            draft.ewayBillNo?.let { "E-way bill $it" },
        ).filterNotNull().joinToString(" · ")
    }

    private fun formatDate(epoch: Long): String =
        java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.ENGLISH).format(java.util.Date(epoch))

    /** FNV-1a 64-bit — stable across JVMs, good enough to prove reprint identity (§8). */
    private fun fnv1a(text: String): Long {
        var hash = -0x340d631b7bdddcdbL
        text.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }

    private fun encodePayload(payload: BiltySnapshotPayload): String = JSONObject().apply {
        put("companyName", payload.companyName)
        put("addressLine", payload.addressLine)
        put("contactLine", payload.contactLine)
        put("consignorName", payload.consignorName)
        put("consignorContact", payload.consignorContact)
        put("consignorGstin", payload.consignorGstin)
        put("consignorAddress", payload.consignorAddress)
        put("consigneeName", payload.consigneeName)
        put("consigneeContact", payload.consigneeContact)
        put("consigneeGstin", payload.consigneeGstin)
        put("consigneeAddress", payload.consigneeAddress)
        put("docNo", payload.docNo)
        put("date", payload.date)
        put("fromStation", payload.fromStation)
        put("toStation", payload.toStation)
        put("packages", payload.packages)
        put("goodsDescription", payload.goodsDescription)
        put("actualWeight", payload.actualWeight)
        put("chargeableWeight", payload.chargeableWeight)
        put("rate", payload.rate)
        put("freight", payload.freight)
        put("hamali", payload.hamali)
        put("doorDelivery", payload.doorDelivery)
        put("taxable", payload.taxable)
        put("gst", payload.gst)
        put("rounding", payload.rounding)
        put("gstLabel", payload.gstLabel)
        put("totalLabel", payload.totalLabel)
        put("grandTotal", payload.grandTotal)
        put("amountInWords", payload.amountInWords)
        put("stamp", payload.stamp)
        put("footer", payload.footer)
        put("provisionalCrossRef", payload.provisionalCrossRef ?: JSONObject.NULL)
    }.toString()

    private fun decodePayload(json: String): BiltySnapshotPayload? = runCatching {
        val o = JSONObject(json)
        BiltySnapshotPayload(
            companyName = o.getString("companyName"),
            addressLine = o.getString("addressLine"),
            contactLine = o.getString("contactLine"),
            consignorName = o.getString("consignorName"),
            consignorContact = o.getString("consignorContact"),
            consignorGstin = o.getString("consignorGstin"),
            consignorAddress = o.getString("consignorAddress"),
            consigneeName = o.getString("consigneeName"),
            consigneeContact = o.getString("consigneeContact"),
            consigneeGstin = o.getString("consigneeGstin"),
            consigneeAddress = o.getString("consigneeAddress"),
            docNo = o.getString("docNo"),
            date = o.getString("date"),
            fromStation = o.getString("fromStation"),
            toStation = o.getString("toStation"),
            packages = o.getString("packages"),
            goodsDescription = o.getString("goodsDescription"),
            actualWeight = o.getString("actualWeight"),
            chargeableWeight = o.getString("chargeableWeight"),
            rate = o.getString("rate"),
            freight = o.getString("freight"),
            hamali = o.getString("hamali"),
            doorDelivery = o.getString("doorDelivery"),
            taxable = o.getString("taxable"),
            gst = o.getString("gst"),
            rounding = o.getString("rounding"),
            gstLabel = o.getString("gstLabel"),
            totalLabel = o.getString("totalLabel"),
            grandTotal = o.getString("grandTotal"),
            amountInWords = o.getString("amountInWords"),
            stamp = o.getString("stamp"),
            footer = o.getString("footer"),
            provisionalCrossRef = o.optString("provisionalCrossRef", null),
        )
    }.getOrNull()
}

    data class ConsignmentPayload(
        val bilty_no: String?,
        val provisional_no: String?,
        val status: String,
        val payment_mode: String,
        val total_paise: Long,
        val booked_at: Long,
        val amends_id: String? = null,
        val amendment_reason: String? = null,
    ) {
        fun toJson(): String = JSONObject()
            .put("bilty_no", bilty_no ?: JSONObject.NULL)
            .put("provisional_no", provisional_no ?: JSONObject.NULL)
            .put("status", status)
            .put("payment_mode", payment_mode)
            .put("total_paise", total_paise)
            .put("booked_at", booked_at)
            .put("amends_id", amends_id ?: JSONObject.NULL)
            .put("amendment_reason", amendment_reason ?: JSONObject.NULL)
            .toString()

        companion object {
            fun from(entity: ConsignmentEntity) = ConsignmentPayload(
                bilty_no = entity.bilty_no,
                provisional_no = entity.provisional_no,
                status = entity.status_projection,
                payment_mode = entity.payment_mode,
                total_paise = entity.total_paise,
                booked_at = entity.booked_at,
                amends_id = entity.amends_id,
                amendment_reason = entity.amendment_reason,
            )
        }
    }

    data class SnapshotPayload(
        val document_type: String,
        val version: Int,
        val content_hash: String,
        val copy_count: Int,
    ) {
        fun toJson(): String = JSONObject()
            .put("document_type", document_type)
            .put("version", version)
            .put("content_hash", content_hash)
            .put("copy_count", copy_count)
            .toString()

        companion object {
            fun from(entity: DocSnapshotEntity) = SnapshotPayload(
                document_type = entity.document_type,
                version = entity.version,
                content_hash = entity.content_hash,
                copy_count = entity.copy_count,
            )
        }
    }
