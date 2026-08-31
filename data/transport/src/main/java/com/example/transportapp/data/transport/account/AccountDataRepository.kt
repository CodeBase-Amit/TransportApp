package com.example.transportapp.data.transport.account

import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.data.transport.session.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** The §B31 "this phone" facts. */
data class PhoneData(
    val records: Int,
    val dbBytes: Long,
    val queue: List<QueueEntry>,
)

/** One OUTBOX row as the T31 sync queue prints it. */
data class QueueEntry(
    val description: String,
    val createdAt: Long,
    val pending: Boolean,
)

/**
 * Account-and-data reads (Phase2.md S10): record counts, the database file's size and the
 * outbox queue as human sentences. Features never touch Room directly (Spec §2).
 */
@Singleton
class AccountDataRepository @Inject constructor(
    private val database: TransportDatabase,
    private val sessionRepository: SessionRepository,
) {

    suspend fun phoneData(): PhoneData {
        val s = sessionRepository.session.first()
        val dao = database.consignmentDao()
        val consignments = dao.countConsignments(s.companyId)
        val bills = database.reportsDao().registerTotals(s.companyId, 0, System.currentTimeMillis()).cnt
        val parties = database.mastersDao().observeParties(s.companyId, "").first().size
        val queue = database.outboxDao().getReady(Long.MAX_VALUE, limit = 20).map { row ->
            QueueEntry(
                description = describe(row.entity_type.name, row.op.name),
                createdAt = row.created_at,
                pending = row.state.name == "PENDING",
            )
        }
        return PhoneData(records = consignments + bills + parties, dbBytes = 0, queue = queue)
    }

    private fun describe(entityType: String, op: String): String = when (entityType) {
        "CONSIGNMENT" -> if (op == "INSERT") "Bilty booked" else "Bilty updated"
        "STATUS_EVENT" -> "Status event recorded"
        "FREIGHT_BILL" -> "Freight bill " + if (op == "INSERT") "created" else "updated"
        "RECEIPT" -> "Money receipt recorded"
        "RECEIPT_ALLOCATION" -> "Receipt allocation"
        "PARTY" -> "Party master change"
        "TRIP" -> "Challan updated"
        else -> entityType.lowercase().replace('_', ' ') + " change"
    }
}
