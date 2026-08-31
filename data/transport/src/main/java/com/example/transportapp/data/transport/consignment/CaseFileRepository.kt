package com.example.transportapp.data.transport.consignment

import com.example.transportapp.core.common.Money
import com.example.transportapp.core.database.dao.ConsignmentDao
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One consignment as one business transaction (Design T8): docket header, the live event
 * timeline (§7.2 log read forwards), the documents, the money position and the record
 * lines. Everything comes from local storage by design — the screen has no full-screen
 * error state.
 */
data class CaseFile(
    val biltyNo: String,
    val status: ConsignmentStatus,
    val paymentMode: PaymentMode?,
    val fromStation: String,
    val toStation: String,
    val distanceKm: Int?,
    val bookedText: String,
    val packages: Long,
    val chargeableKg: Long,
    val expectedText: String,
    val events: List<CaseTimelineEvent>,
    val documents: List<CaseDocument>,
    val money: List<CaseMoneyLine>,
    val toPayCallout: String?,
    val recordLines: List<String>,
)

data class CaseTimelineEvent(
    val type: String,
    val location: String?,
    val atText: String?,
    val actor: String,
    val remark: String?,
    val challanRef: String?,
    /** An unreached tick (e.g. the expected Arrived row) renders differently. */
    val unreached: Boolean = false,
)

data class CaseDocument(
    val kind: String,
    val title: String,
    val number: String?,
    val trailing: String?,
    val actionable: Boolean,
)

data class CaseMoneyLine(val label: String, val amountPaise: Long, val strong: Boolean = false)

/** Case-file reads (Phase2.md S6). */
interface CaseFileRepository {

    suspend fun caseFile(companyId: String, biltyNo: String, branchName: String, now: Long): CaseFile?
}

@Singleton
class CaseFileRepositoryImpl @Inject constructor(
    private val consignmentDao: ConsignmentDao,
    private val mastersDao: com.example.transportapp.core.database.dao.MastersDao,
) : CaseFileRepository {

    override suspend fun caseFile(companyId: String, biltyNo: String, branchName: String, now: Long): CaseFile? {
        val consignment = consignmentDao.getConsignmentByBiltyNo(companyId, biltyNo)
            ?: consignmentDao.getConsignmentByProvisionalNo(companyId, biltyNo)
            ?: return null
        val status = runCatching { ConsignmentStatus.valueOf(consignment.status_projection) }
            .getOrDefault(ConsignmentStatus.BOOKED)
        val paymentMode = runCatching { PaymentMode.valueOf(consignment.payment_mode) }.getOrNull()
        val fromStation = mastersDao.getStation(consignment.from_station_id)?.name ?: ""
        val toStation = mastersDao.getStation(consignment.to_station_id)?.name ?: ""
        val route = mastersDao.getRoute(consignment.route_id)
        val events = consignmentDao.getEvents(consignment.local_id)
        val lines = consignmentDao.getChargeLines(consignment.local_id)
        val items = consignmentDao.getItems(consignment.local_id)
        val snapshot = consignmentDao.getLatestSnapshot(consignment.local_id)

        val chargeableKg = items.maxOfOrNull { it.chargeable_weight_g }?.let { it / 1000 }
            ?: consignment.chargeable_weight_g / 1000

        val timeline = buildList {
            events.forEach { event ->
                add(
                    CaseTimelineEvent(
                        type = event.event_type.lowercase().replaceFirstChar { it.uppercase() }
                            .replace('_', ' '),
                        location = event.location,
                        atText = formatDateTime(event.occurred_at),
                        actor = event.actor_name,
                        remark = event.remark,
                        challanRef = event.challan_ref,
                    ),
                )
            }
            unreachedNext(status, toStation, consignment.expected_arrival)?.let { add(it) }
        }

        val freight = consignment.freight_paise
        val charges = lines
            .filter { it.head_code !in setOf("freight", "gst", "rounding") }
            .sumOf { it.computed_paise }
        val money = buildList {
            add(CaseMoneyLine("Freight", freight))
            add(CaseMoneyLine("Charges", charges))
            add(CaseMoneyLine("GST", consignment.gst_paise))
            add(CaseMoneyLine("Total to collect", consignment.total_paise, strong = true))
        }
        val toPayCallout = if (paymentMode == PaymentMode.TOPAY) {
            "To Pay — collect ${Money(consignment.total_paise).formatted()} at $toStation before handing over the goods."
        } else {
            null
        }

        val documents = listOf(
            CaseDocument("bilty", "Bilty", consignment.bilty_no ?: consignment.provisional_no, "${snapshot?.copy_count ?: 4} copies", actionable = true),
            CaseDocument(
                "challan",
                "Loading challan",
                events.firstNotNullOfOrNull { it.challan_ref },
                if (events.any { it.challan_ref != null }) null else "Not issued yet",
                actionable = events.any { it.challan_ref != null },
            ),
            CaseDocument("freight_bill", "Freight bill", null, if (consignment.freight_bill_id == null) "Not raised yet" else null, actionable = consignment.freight_bill_id == null),
            CaseDocument("pod", "POD", null, if (status == ConsignmentStatus.DELIVERED) "Captured" else "Pending delivery", actionable = false),
        )

        val bookedText = "booked ${formatDateTime(consignment.booked_at)} by ${consignment.booked_by_name}"
        val recordLines = listOf(
            "Booked at $branchName by ${consignment.booked_by_name} · last change ${syncText(consignment.updated_at_local, consignment.sync_state.name, now)}",
            "Snapshot v${snapshot?.version ?: 1} · template ${snapshot?.template_id ?: "tpl-bilty-default"} — reprints will match the copies already issued.",
        )

        return CaseFile(
            biltyNo = consignment.bilty_no ?: consignment.provisional_no ?: biltyNo,
            status = status,
            paymentMode = paymentMode,
            fromStation = fromStation,
            toStation = toStation,
            distanceKm = route?.distance_km,
            bookedText = bookedText,
            packages = consignment.packages,
            chargeableKg = chargeableKg,
            expectedText = formatDate(consignment.expected_arrival),
            events = timeline,
            documents = documents,
            money = money,
            toPayCallout = toPayCallout,
            recordLines = recordLines,
        )
    }

    /**
     * The unreached tick at the bottom of the timeline: the delivery milestone the
     * consignment has not reached yet, with the expected date (Design T8).
     */
    private fun unreachedNext(status: ConsignmentStatus, toStation: String, expectedAt: Long): CaseTimelineEvent? = when (status) {
        ConsignmentStatus.DRAFT, ConsignmentStatus.ARRIVED, ConsignmentStatus.OUT_FOR_DELIVERY ->
            CaseTimelineEvent("Delivered", null, null, "", null, null, unreached = true).takeIf { status != ConsignmentStatus.DRAFT }
        ConsignmentStatus.DELIVERED, ConsignmentStatus.CANCELLED, ConsignmentStatus.RETURNED -> null
        else -> CaseTimelineEvent("Arrived", toStation, "expected ${formatDate(expectedAt)}", "", null, null, unreached = true)
    }

    private fun syncText(updatedAt: Long, syncState: String, now: Long): String = when (syncState) {
        "PENDING" -> "pending sync"
        else -> "synced ${formatDateTime(updatedAt)}"
    }

    private fun formatDateTime(epoch: Long): String =
        java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.ENGLISH).format(java.util.Date(epoch))

    private fun formatDate(epoch: Long): String =
        java.text.SimpleDateFormat("d MMM", java.util.Locale.ENGLISH).format(java.util.Date(epoch))
}
