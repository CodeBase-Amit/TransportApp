package com.example.transportapp.data.transport.reports

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.export.engine.BiltyRegisterRow
import com.example.transportapp.export.engine.CsvWriter
import com.example.transportapp.data.transport.session.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** One freight-register row as the viewer and the CSV print it. */
data class FreightRegisterRow(
    val biltyNo: String,
    val bookedAt: Long,
    val branch: String,
    val consignor: String,
    val consignee: String,
    val route: String,
    val packages: Long,
    val weightKg: Long,
    val freightPaise: Long,
    val gstPaise: Long,
    val totalPaise: Long,
    val cancelled: Boolean,
)

/** The register's totals band (§B22). */
data class FreightRegisterTotals(
    val rows: Int,
    val packages: Long,
    val weightKg: Long,
    val freightPaise: Long,
    val gstPaise: Long,
    val totalPaise: Long,
)

/** One T21 reports-hub row with its cached headline figure. */
data class ReportEntry(
    val id: String,
    val group: String,
    val label: String,
    val description: String,
    /** Rupee headline, pre-formatted; null where the design shows none. */
    val headline: String?,
    val minRole: String = "ACCOUNTANT",
)

/** The export centre's per-sheet row counts (§B23). */
data class SheetCountUi(
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
 * Reports and exports (Phase2.md S10, §13/§14). The freight register is one query; the hub's
 * aggregates run in parallel. Exports write CSV into the app's files dir — the share sheet
 * (SAF) lands in the UI layer; XLSX is Phase-2-out-of-scope and answers OFFLINE_UNAVAILABLE.
 */
interface ReportsRepository {

    suspend fun freightRegister(companyWide: Boolean, from: Long, to: Long): Pair<List<FreightRegisterRow>, FreightRegisterTotals>

    suspend fun freightRegisterAsCsvRows(rows: List<FreightRegisterRow>): List<BiltyRegisterRow>

    /** The register as a deterministic CSV document (§14). */
    suspend fun freightRegisterCsv(companyWide: Boolean, from: Long, to: Long): String

    suspend fun hubEntries(now: Long): List<ReportEntry>

    suspend fun revenueByRoute(from: Long, to: Long): List<Triple<String, Long, Int>>

    suspend fun outstandingByParty(now: Long): List<Triple<String, Long, Long>>

    suspend fun buildCsvExport(sheetName: String, csv: String, now: Long): Result<File>

    /** The §B23 pack: one CSV per selected sheet, zipped into the app's files dir. */
    suspend fun buildCsvPack(sheets: List<Pair<String, String>>, now: Long): Result<File>

    /** Recent exports: the files this device has built, newest first. */
    fun recentExports(): List<File>

    /** XLSX is explicitly out of scope for Phase 2 (Phase2.md §10). */
    suspend fun buildXlsxExport(sheetName: String, now: Long): Result<Nothing>

    suspend fun sheetCounts(): SheetCountUi

    suspend fun registerCsvForPeriod(from: Long, to: Long): String
}

@Singleton
class ReportsRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val sessionRepository: SessionRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ReportsRepository {

    override suspend fun freightRegister(companyWide: Boolean, from: Long, to: Long): Pair<List<FreightRegisterRow>, FreightRegisterTotals> {
        val s = sessionRepository.session.first()
        val rows = database.reportsDao().freightRegister(s.companyId, if (companyWide) null else s.branchId, from, to)
        val totals = FreightRegisterTotals(
            rows = rows.size,
            packages = rows.sumOf { it.packages },
            weightKg = rows.sumOf { it.weight_kg },
            freightPaise = rows.sumOf { it.freight_paise },
            gstPaise = rows.sumOf { it.gst_paise },
            totalPaise = rows.sumOf { it.total_paise },
        )
        val mapped = rows.map {
            FreightRegisterRow(
                biltyNo = it.display_no, bookedAt = it.booked_at, branch = it.branch,
                consignor = it.consignor, consignee = it.consignee, route = it.route,
                packages = it.packages, weightKg = it.weight_kg,
                freightPaise = it.freight_paise, gstPaise = it.gst_paise, totalPaise = it.total_paise,
                cancelled = it.cancelled,
            )
        }
        return mapped to totals
    }

    override suspend fun freightRegisterAsCsvRows(rows: List<FreightRegisterRow>): List<BiltyRegisterRow> =
        rows.map {
            BiltyRegisterRow(
                biltyNo = it.biltyNo, bookedAt = it.bookedAt, branch = it.branch,
                consignor = it.consignor, consignee = it.consignee, route = it.route,
                packages = it.packages, weightKg = it.weightKg,
                freightPaise = it.freightPaise, gstPaise = it.gstPaise, totalPaise = it.totalPaise,
                cancelled = it.cancelled,
            )
        }

    override suspend fun freightRegisterCsv(companyWide: Boolean, from: Long, to: Long): String {
        val (rows, _) = freightRegister(companyWide, from, to)
        return CsvWriter.writeBiltyRegister(freightRegisterAsCsvRows(rows))
    }

    override suspend fun hubEntries(now: Long): List<ReportEntry> = coroutineScope {
        val s = sessionRepository.session.first()
        val dao = database.reportsDao()
        val fyStart = fyStart(now)
        val deferred = listOf(
            async(Dispatchers.IO) { "freight_register" to dao.registerTotals(s.companyId, fyStart, now).freightPaise },
            async(Dispatchers.IO) { "outstanding" to dao.outstandingTotal(s.companyId, now) },
            async(Dispatchers.IO) { "topay_pending" to dao.topayPendingTotal(s.companyId) },
            async(Dispatchers.IO) { "lorry_hire" to dao.lorryHireTotal(s.companyId, fyStart, now) },
            async(Dispatchers.IO) { "number_gaps" to dao.numberGaps(s.companyId, fyStart, now) },
        ).awaitAll().toMap()
        listOf(
            ReportEntry("freight_register", "HOW MUCH DID WE EARN", "Freight register", "every bilty with its charges", rupees(deferred["freight_register"] ?: 0L)),
            ReportEntry("revenue_by_route", "HOW MUCH DID WE EARN", "Revenue by route", "which lanes actually pay", null),
            ReportEntry("revenue_by_party", "HOW MUCH DID WE EARN", "Revenue by party", "your top twenty customers", null),
            ReportEntry("revenue_by_branch", "HOW MUCH DID WE EARN", "Revenue by branch", "Indore, Nagpur, Bhiwandi side by side", null),
            ReportEntry("outstanding_by_party", "WHAT DO PEOPLE OWE US", "Outstanding by party", "with ageing buckets", rupees(deferred["outstanding"] ?: 0L)),
            ReportEntry("topay_pending", "WHAT DO PEOPLE OWE US", "To Pay pending", "goods delivered, money not collected", rupees(deferred["topay_pending"] ?: 0L)),
            ReportEntry("ageing_summary", "WHAT DO PEOPLE OWE US", "Ageing summary", "0-30, 31-60, 61-90, over 90", null),
            ReportEntry("lorry_hire_register", "WHAT DID IT COST US", "Lorry hire register", "every challan with hire and balance", rupees(deferred["lorry_hire"] ?: 0L)),
            ReportEntry("vehicle_cost", "WHAT DID IT COST US", "Vehicle-wise cost", "own vehicles against attached", null),
            ReportEntry("driver_advances", "WHAT DID IT COST US", "Driver advances outstanding", "who is holding company money", null),
            ReportEntry("gst_outward", "WHAT THE DEPARTMENT WILL ASK", "GST outward summary", "taxable value and tax by rate", null),
            ReportEntry("gstr1_b2b", "WHAT THE DEPARTMENT WILL ASK", "GSTR-1 style B2B listing", "invoice-wise with GSTINs", null),
            ReportEntry("income_expense", "WHAT THE DEPARTMENT WILL ASK", "Income and expense summary", "for the return", null),
            ReportEntry(
                "number_continuity", "WHAT THE DEPARTMENT WILL ASK", "Document number continuity", "gaps and cancellations",
                headline = if ((deferred["number_gaps"] ?: 0L) == 0L) "No gaps" else "${deferred["number_gaps"]} cancelled",
            ),
        )
    }

    override suspend fun revenueByRoute(from: Long, to: Long): List<Triple<String, Long, Int>> {
        val s = sessionRepository.session.first()
        return database.reportsDao().revenueByRoute(s.companyId, from, to).map { Triple(it.route, it.freightPaise, it.cnt) }
    }

    override suspend fun outstandingByParty(now: Long): List<Triple<String, Long, Long>> {
        val s = sessionRepository.session.first()
        return database.reportsDao().outstandingByParty(s.companyId, now).map { Triple(it.party, it.outstandingPaise, it.over90Paise) }
    }

    override suspend fun buildCsvExport(sheetName: String, csv: String, now: Long): Result<File> =
        try {
            val s = sessionRepository.session.first()
            val dir = File(appContext.filesDir, "exports").apply { mkdirs() }
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ENGLISH).format(java.util.Date(now))
            val file = File(dir, "${s.companyName.replace(" ", "")}-$sheetName-$stamp.csv")
            file.writeText(csv, Charsets.UTF_8)
            Result.success(file)
        } catch (io: java.io.IOException) {
            Result.failure(ErrorCode.EXPORT_TOO_LARGE, "The export could not be written: ${io.message}")
        }

    override suspend fun buildCsvPack(sheets: List<Pair<String, String>>, now: Long): Result<File> =
        try {
            val s = sessionRepository.session.first()
            val dir = File(appContext.filesDir, "exports").apply { mkdirs() }
            val stamp = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ENGLISH).format(java.util.Date(now))
            val zip = File(dir, "${s.companyName.replace(" ", "")}-pack-$stamp.zip")
            java.util.zip.ZipOutputStream(zip.outputStream().buffered()).use { zos ->
                sheets.forEach { (name, csv) ->
                    zos.putNextEntry(java.util.zip.ZipEntry("$name.csv"))
                    zos.write(csv.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }
            Result.success(zip)
        } catch (io: java.io.IOException) {
            Result.failure(ErrorCode.EXPORT_TOO_LARGE, "The pack could not be written: ${io.message}")
        }

    override fun recentExports(): List<File> =
        File(appContext.filesDir, "exports").listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()

    override suspend fun sheetCounts(): SheetCountUi {
        val s = sessionRepository.session.first()
        val c = database.reportsDao().sheetCounts(s.companyId)
        return SheetCountUi(
            register = c.register, chargeLines = c.chargeLines, bills = c.bills,
            receipts = c.receipts, allocations = c.allocations, topayCollections = c.topayCollections,
            trips = c.trips, parties = c.parties,
        )
    }

    override suspend fun registerCsvForPeriod(from: Long, to: Long): String =
        freightRegisterCsv(companyWide = true, from = from, to = to)

    override suspend fun buildXlsxExport(sheetName: String, now: Long): Result<Nothing> =
        Result.failure(ErrorCode.OFFLINE_UNAVAILABLE, "The Excel pack ships with the online tier — CSV has everything the CA needs today")

    private fun rupees(paise: Long): String =
        com.example.transportapp.core.common.formatIndianGrouping(paise / 100) + "." + (paise % 100).toString().padStart(2, '0')

    private fun fyStart(now: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(java.util.Calendar.MONTH, java.util.Calendar.APRIL)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0); cal.set(java.util.Calendar.MILLISECOND, 0)
        if (cal.after(java.util.Date(now))) cal.add(java.util.Calendar.YEAR, -1)
        return cal.timeInMillis
    }
}
