package com.example.transportapp.data.transport.tracking

import androidx.room.withTransaction
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.entity.AttachmentEntity
import com.example.transportapp.core.database.entity.PodEntity
import com.example.transportapp.core.database.entity.StatusEventEntity
import com.example.transportapp.core.database.envelope.SyncState
import com.example.transportapp.core.database.outbox.OutboxEntityType
import com.example.transportapp.core.database.outbox.OutboxOp
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.tracking.Ageing
import com.example.transportapp.domain.transport.consignment.ConsignmentStateMachine
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** What the T9 sheet saves (§7.2's event record minus the photo, which rides separately). */
data class NewStatusEvent(
    val biltyNo: String,
    val eventType: String,
    val location: String? = null,
    val remark: String? = null,
    val reasonCode: String? = null,
    val challanRef: String? = null,
    /** The idempotency key: the same client_event_id replays as a no-op (§3.4 #8). */
    val clientEventId: String = UUID.randomUUID().toString(),
)

/** One row of the T4 exception strip (§13). */
data class ExceptionItem(
    val biltyNo: String,
    val eventType: String,
    val reasonCode: String?,
    val remark: String?,
    val atText: String,
    val isLate: Boolean,
)

/**
 * Status events and tracking (Phase2.md S8). The log is append-only (§3.4 #7 — no update or
 * delete path exists); every append validates the §7.1 transition, the §7.2 field rules and
 * the §7.1 delivery gates, then moves the projection with it (D1). Replay of an existing
 * `client_event_id` is a no-op, not an error (§3.4 #8).
 */
interface StatusRepository {

    suspend fun append(event: NewStatusEvent, now: Long): Result<Unit>

    /** Same log → same projection, always (§16.1). */
    suspend fun rebuildProjection(consignmentId: String): ConsignmentStatus?

    /** One transaction, one event per consignment on the challan — all or nothing (§7.2). */
    suspend fun bulkAppendByChallan(challanNo: String, eventType: String, now: Long): Result<Int>

    /** The legal §7.1 continuations, as the T9 sheet renders them. */
    suspend fun legalNext(biltyNo: String): List<ConsignmentStatus>

    /** The consignment's current projected status — the T9 sheet's context line. */
    suspend fun currentStatus(biltyNo: String): ConsignmentStatus?

    suspend fun recordPod(biltyNo: String, consigneeName: String, signatureRef: String?, photoRef: String?, remarks: String?, now: Long): Result<Unit>

    suspend fun addAttachment(biltyNo: String, kind: String, fileRef: String, sizeBytes: Long, caption: String?, now: Long): Result<Unit>

    /**
     * The §12.1 To Pay Manager waiver: an append-only `WAIVE_TOPAY` audit event that the
     * projection fold ignores (an audit marker, not a status change). Only Owner/Manager.
     */
    suspend fun waiveTopPay(biltyNo: String, reason: String, now: Long): Result<Unit>

    suspend fun exceptions(companyId: String, branchId: String?, sinceAt: Long, now: Long): List<ExceptionItem>

    suspend fun countOverdue(companyId: String, branchId: String?, now: Long): Int
}

@Singleton
class StatusRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val sessionRepository: SessionRepository,
    private val outboxWriter: OutboxWriter,
) : StatusRepository {

    override suspend fun append(event: NewStatusEvent, now: Long): Result<Unit> = database.withTransaction {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")

        val consignment = database.consignmentDao().getConsignmentByBiltyNo(session.companyId, event.biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(session.companyId, event.biltyNo)
            ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No bilty ${event.biltyNo} on this device")

        // §3.4 #8: the same client_event_id replaying is a no-op.
        if (database.consignmentDao().getEventsOrdered(consignment.local_id).any { it.client_event_id == event.clientEventId }) {
            return@withTransaction Result.success(Unit)
        }

        val from = runCatching { ConsignmentStatus.valueOf(consignment.status_projection) }.getOrDefault(ConsignmentStatus.BOOKED)
        val to = targetOf(event.eventType)
            ?: return@withTransaction Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "Unknown event type ${event.eventType}")

        if (!ConsignmentStateMachine.canTransition(from, to)) {
            return@withTransaction Result.failure(
                ErrorCode.CONSIGNMENT_IMMUTABLE,
                "A consignment in status ${from.wording} cannot move to ${to.wording}",
            )
        }

        // §7.2: a Held event needs a reason code and a remark of at least ten characters.
        if (to == ConsignmentStatus.HELD) {
            val reasonOk = event.reasonCode in setOf("SHORTAGE", "DAMAGE", "DETAINED", "OTHER")
            val remarkOk = (event.remark?.length ?: 0) >= 10
            if (!reasonOk) return@withTransaction Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "A hold needs a reason: Shortage, Damage, Detained or Other")
            if (!remarkOk) return@withTransaction Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "A hold needs a remark of at least ten characters")
        }

        // §7.1: Delivered needs a POD record (or a Manager waiver); a To Pay delivery also
        // needs the money settled — collected (an explicit allocation), waived by audit
        // (waiveTopPay), or the acting Manager (§12.1, §18.3 TOPAY_UNCOLLECTED).
        if (to == ConsignmentStatus.DELIVERED) {
            val pod = database.consignmentDao().getPod(consignment.local_id)
            val isManager = session.role == "OWNER" || session.role == "MANAGER"
            if (pod == null && !isManager) {
                return@withTransaction Result.failure(ErrorCode.POD_REQUIRED, "Capture the POD before marking delivered")
            }
            if (consignment.payment_mode == "TOPAY" && !isManager) {
                val collected = database.billingDao().sumTopayAllocated(consignment.local_id) >= consignment.total_paise
                val waived = database.consignmentDao().getEventsOrdered(consignment.local_id).any { it.event_type == WAIVE_EVENT_TYPE }
                if (!collected && !waived) {
                    return@withTransaction Result.failure(
                        ErrorCode.TOPAY_UNCOLLECTED,
                        "To Pay ${(consignment.total_paise / 100.0).format2()} must be collected (or waived by a manager) first",
                    )
                }
            }
        }

        insertEvent(consignment.local_id, session.companyId, event, session.name, session.branchId, now)
        // The projection re-folds the log rather than trusting the requested target: a
        // back-dated event (§7.2 lets occurred_at be user-set within a window) must leave
        // the projection as the fold of the whole log, never ahead of it (D1, §3.4 #3).
        val projected = derive(consignment.local_id) ?: to
        database.consignmentDao().upsertConsignment(consignment.copy(status_projection = projected.name, updated_at_local = now))
        Result.success(Unit)
    }

    override suspend fun rebuildProjection(consignmentId: String): ConsignmentStatus? {
        val consignment = database.consignmentDao().getConsignment(consignmentId) ?: return null
        val derived = derive(consignment.local_id)
        if (derived != null) {
            database.consignmentDao().upsertConsignment(consignment.copy(status_projection = derived.name, updated_at_local = System.currentTimeMillis()))
        }
        return derived
    }

    override suspend fun bulkAppendByChallan(challanNo: String, eventType: String, now: Long): Result<Int> = database.withTransaction {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        val to = targetOf(eventType)
            ?: return@withTransaction Result.failure(ErrorCode.CONSIGNMENT_IMMUTABLE, "Unknown event type $eventType")
        val trip = database.tripDao().getTripByChallanNo(session.companyId, challanNo)
            ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No challan $challanNo on this device")

        // §7.2: any illegal transition aborts the whole challan — a half-dispatched challan
        // is worse than an undispatched one.
        val planned = mutableListOf<Pair<String, ConsignmentStatus>>()
        database.tripDao().getLegRows(trip.local_id).forEach { leg ->
            val consignment = database.consignmentDao().getConsignment(leg.consignment_id) ?: return@forEach
            val from = runCatching { ConsignmentStatus.valueOf(consignment.status_projection) }.getOrDefault(ConsignmentStatus.BOOKED)
            if (!ConsignmentStateMachine.canTransition(from, to)) {
                return@withTransaction Result.failure(
                    ErrorCode.CONSIGNMENT_IMMUTABLE,
                    "A consignment on this challan is ${from.wording}; it cannot move to ${to.wording}. Remove it from the load, then retry.",
                )
            }
            planned.add(leg.consignment_id to to)
        }
        if (planned.isEmpty()) return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No consignments on challan $challanNo")

        planned.forEach { (id, target) ->
            insertEvent(id, session.companyId, NewStatusEvent(biltyNo = id, eventType = eventType, clientEventId = UUID.randomUUID().toString()), session.name, session.branchId, now)
            val consignment = database.consignmentDao().getConsignment(id)
            if (consignment != null) {
                database.consignmentDao().upsertConsignment(consignment.copy(status_projection = target.name, updated_at_local = now))
            }
        }
        Result.success(planned.size)
    }

    override suspend fun legalNext(biltyNo: String): List<ConsignmentStatus> {
        val session = sessionRepository.session.first()
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(session.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(session.companyId, biltyNo)
            ?: return emptyList()
        val from = runCatching { ConsignmentStatus.valueOf(consignment.status_projection) }.getOrDefault(ConsignmentStatus.BOOKED)
        return ConsignmentStateMachine.allowed(from).toList()
    }

    override suspend fun currentStatus(biltyNo: String): ConsignmentStatus? {
        val session = sessionRepository.session.first()
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(session.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(session.companyId, biltyNo)
            ?: return null
        return runCatching { ConsignmentStatus.valueOf(consignment.status_projection) }.getOrNull()
    }

    override suspend fun recordPod(biltyNo: String, consigneeName: String, signatureRef: String?, photoRef: String?, remarks: String?, now: Long): Result<Unit> {
        val session = sessionRepository.session.first()
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(session.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(session.companyId, biltyNo)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No bilty $biltyNo on this device")
        database.consignmentDao().upsertPod(
            PodEntity(
                local_id = "pod-" + UUID.randomUUID().toString(), server_id = null,
                updated_at_local = now, updated_at_server = null, sync_state = SyncState.PENDING, deleted_at = null,
                consignment_id = consignment.local_id, consignee_name = consigneeName,
                signature_ref = signatureRef, photo_ref = photoRef, pod_date = now, remarks = remarks,
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun addAttachment(biltyNo: String, kind: String, fileRef: String, sizeBytes: Long, caption: String?, now: Long): Result<Unit> {
        val session = sessionRepository.session.first()
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(session.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(session.companyId, biltyNo)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No bilty $biltyNo on this device")
        database.consignmentDao().upsertAttachment(
            AttachmentEntity(
                local_id = "att-" + UUID.randomUUID().toString(), server_id = null,
                updated_at_local = now, updated_at_server = null, sync_state = SyncState.PENDING, deleted_at = null,
                consignment_id = consignment.local_id, kind = kind, file_ref = fileRef,
                size_bytes = sizeBytes, caption = caption,
            ),
        )
        return Result.success(Unit)
    }

    override suspend fun waiveTopPay(biltyNo: String, reason: String, now: Long): Result<Unit> = database.withTransaction {
        val session = sessionRepository.session.first()
        if (!session.isSignedIn) return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "No active session")
        if (session.role != "OWNER" && session.role != "MANAGER") {
            return@withTransaction Result.failure(ErrorCode.AUTH_NO_ACCESS, "Only a Manager can waive a To Pay collection")
        }
        if (reason.isBlank()) {
            return@withTransaction Result.failure(ErrorCode.TOPAY_UNCOLLECTED, "A waiver needs a reason for the audit trail")
        }
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(session.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(session.companyId, biltyNo)
            ?: return@withTransaction Result.failure(ErrorCode.MASTER_IN_USE, "No bilty $biltyNo on this device")
        if (consignment.payment_mode != "TOPAY") {
            return@withTransaction Result.failure(ErrorCode.TOPAY_UNCOLLECTED, "Only To Pay consignments can be waived")
        }
        insertAuditEvent(
            consignmentId = consignment.local_id,
            companyId = session.companyId,
            eventType = WAIVE_EVENT_TYPE,
            remark = reason.trim(),
            actorName = session.name,
            branchId = session.branchId,
            now = now,
        )
        Result.success(Unit)
    }

    override suspend fun exceptions(companyId: String, branchId: String?, sinceAt: Long, now: Long): List<ExceptionItem> =
        database.consignmentDao().getRecentHeldEvents(companyId, branchId, sinceAt).map { event ->
            val consignment = database.consignmentDao().getConsignment(event.consignment_id)
            val late = consignment?.let {
                Ageing.isOverdue(it.expected_arrival, now)
            } ?: false
            ExceptionItem(
                biltyNo = consignment?.bilty_no ?: consignment?.provisional_no ?: "",
                eventType = event.event_type,
                reasonCode = event.reason_code,
                remark = event.remark,
                atText = format(event.occurred_at),
                isLate = late,
            )
        }

    override suspend fun countOverdue(companyId: String, branchId: String?, now: Long): Int =
        database.consignmentDao().countOverdue(companyId, branchId, now - Ageing.DEFAULT_GRACE_DAYS * Ageing.DAY_MS)

    // ── helpers ─────────────────────────────────────────────────────────

    /**
     * An audit event that is not a status change (D40): `WAIVE_TOPAY` rides the append-only
     * log and the outbox like any event, but the projection fold skips it (unknown target).
     */
    private suspend fun insertAuditEvent(consignmentId: String, companyId: String, eventType: String, remark: String, actorName: String, branchId: String, now: Long) {
        val clientEventId = UUID.randomUUID().toString()
        database.consignmentDao().insertStatusEvent(
            StatusEventEntity(
                local_id = "ev-" + UUID.randomUUID().toString(), server_id = null, updated_at_local = now, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null, company_id = companyId,
                consignment_id = consignmentId, client_event_id = clientEventId,
                event_type = eventType, occurred_at = now, recorded_at = now,
                actor_member_id = null, actor_name = actorName, branch_id = branchId,
                location = null, photo_ref = null, reason_code = "MANAGER_WAIVER",
                remark = remark, challan_ref = null,
            ),
        )
        outboxWriter.enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.STATUS_EVENT,
            entityLocalId = consignmentId,
            payloadJson = JSONObject()
                .put("consignment_id", consignmentId)
                .put("client_event_id", clientEventId)
                .put("event_type", eventType)
                .put("occurred_at", now)
                .put("actor_name", actorName)
                .put("branch_id", branchId)
                .put("reason_code", "MANAGER_WAIVER")
                .put("remark", remark)
                .toString(),
            now = now,
        )
    }

    private suspend fun insertEvent(consignmentId: String, companyId: String, event: NewStatusEvent, actorName: String, branchId: String, now: Long) {
        val localId = "ev-" + UUID.randomUUID().toString()
        database.consignmentDao().insertStatusEvent(
            StatusEventEntity(
                local_id = localId, server_id = null, updated_at_local = now, updated_at_server = null,
                sync_state = SyncState.PENDING, deleted_at = null, company_id = companyId,
                consignment_id = consignmentId, client_event_id = event.clientEventId,
                event_type = event.eventType, occurred_at = now, recorded_at = now,
                actor_member_id = null, actor_name = actorName, branch_id = branchId,
                location = event.location, photo_ref = null, reason_code = event.reasonCode,
                remark = event.remark, challan_ref = event.challanRef,
            ),
        )
        outboxWriter.enqueue(
            op = OutboxOp.INSERT,
            entityType = OutboxEntityType.STATUS_EVENT,
            entityLocalId = consignmentId,
            payloadJson = JSONObject()
                .put("consignment_id", consignmentId)
                .put("client_event_id", event.clientEventId)
                .put("event_type", event.eventType)
                .put("occurred_at", now)
                .put("actor_name", actorName)
                .put("branch_id", branchId)
                .put("location", event.location ?: JSONObject.NULL)
                .put("reason_code", event.reasonCode ?: JSONObject.NULL)
                .put("remark", event.remark ?: JSONObject.NULL)
                .toString(),
            now = now,
        )
    }

    private suspend fun derive(consignmentId: String): ConsignmentStatus? {
        val events = database.consignmentDao().getEventsOrdered(consignmentId)
        if (events.isEmpty()) return null
        var current: ConsignmentStatus? = ConsignmentStatus.BOOKED
        events.forEach { event ->
            val target = targetOf(event.event_type) ?: return@forEach
            if (current != null && ConsignmentStateMachine.canTransition(current, target)) {
                current = target
            }
        }
        return current
    }

    private fun targetOf(eventType: String): ConsignmentStatus? = when (eventType.uppercase()) {
        "BOOKED" -> ConsignmentStatus.BOOKED
        "LOADED" -> ConsignmentStatus.LOADED
        "IN_TRANSIT" -> ConsignmentStatus.IN_TRANSIT
        "AT_HUB" -> ConsignmentStatus.AT_HUB
        "ARRIVED" -> ConsignmentStatus.ARRIVED
        "OUT_FOR_DELIVERY" -> ConsignmentStatus.OUT_FOR_DELIVERY
        "DELIVERED" -> ConsignmentStatus.DELIVERED
        "HELD" -> ConsignmentStatus.HELD
        "RETURNED" -> ConsignmentStatus.RETURNED
        "CANCELLED" -> ConsignmentStatus.CANCELLED
        else -> null
    }

    private fun format(epoch: Long): String =
        java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.ENGLISH).format(java.util.Date(epoch))

    private fun Double.format2(): String = java.lang.String.format(java.util.Locale.ENGLISH, "%,.2f", this)

    companion object {
        const val WAIVE_EVENT_TYPE = "WAIVE_TOPAY"
    }
}
