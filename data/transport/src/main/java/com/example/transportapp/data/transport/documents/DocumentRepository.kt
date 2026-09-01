package com.example.transportapp.data.transport.documents

import android.content.Context
import android.net.Uri
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.doc.engine.HtmlRenderer
import com.example.transportapp.doc.engine.TemplateParser
import com.example.transportapp.doc.engine.TemplateParser.ParseResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** A rendered document ready for distribution. */
data class RenderedDocument(
    val pdfBytes: ByteArray,
    val fileName: String,
    val htmlHash: String,
) {
    override fun equals(other: Any?): Boolean = other is RenderedDocument &&
        pdfBytes.contentEquals(other.pdfBytes) && fileName == other.fileName
    override fun hashCode(): Int = 31 * pdfBytes.contentHashCode() + fileName.hashCode()
}

/**
 * Documents (Phase 3 S13, §17.2): render a bilty's stored snapshot through its **pinned
 * template version** — never today's (§9.12) — into vector PDF bytes, then distribute.
 * Reprinting never re-runs data capture, never re-reserves a number, and never recalculates
 * from current rules: the totals in the snapshot are the totals on paper.
 */
interface DocumentRepository {

    /**
     * Render the bilty as one paginated PDF containing one sheet per copy. Fails with
     * `TEMPLATE_VERSION_MISSING` when the pinned version is not on this device (§17.2: a
     * document first printed elsewhere needs one online moment), and surfaces the
     * three-retry PDF failure as `EXPORT_TOO_LARGE`'s retryable cousin.
     */
    suspend fun renderBilty(biltyNo: String, copies: List<String>): Result<RenderedDocument>

    /** The stored snapshot's payload as the renderer's flat value map. */
    suspend fun biltyValues(biltyNo: String): Result<Map<String, String>>?

    suspend fun copyLabels(biltyNo: String): List<String>

    /** Share an already-rendered document (the file name is what the recipient sees). */
    fun share(document: RenderedDocument, chooserTitle: String)

    /** Print an already-rendered document through the byte adapter (§9.8). */
    fun print(document: RenderedDocument)

    /** Save to the public Downloads collection; returns the content URI or null. */
    fun saveToDownloads(document: RenderedDocument): Uri?

    fun renderFromFile(file: File): ByteArray = if (file.exists()) file.readBytes() else ByteArray(0)
}

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val database: TransportDatabase,
    private val sessionRepository: SessionRepository,
    private val pdfPort: PdfPort,
    private val pdfActions: PdfActions,
) : DocumentRepository {

    override suspend fun renderBilty(biltyNo: String, copies: List<String>): Result<RenderedDocument> {
        val s = sessionRepository.session.first()
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(s.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(s.companyId, biltyNo)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No bilty $biltyNo on this device")
        val snapshot = database.consignmentDao().getLatestSnapshot(consignment.local_id)
            ?: return Result.failure(ErrorCode.MASTER_IN_USE, "No snapshot stored for $biltyNo")

        // §9.12: resolve the template at the PINNED version. The snapshot stores the version
        // string it was created against; falling back to today's active row would produce a
        // document that does not match the copy the customer holds.
        val pinnedVersion = snapshot.template_version.toIntOrNull()
            ?: return Result.failure(ErrorCode.TEMPLATE_VERSION_MISSING, "The snapshot carries no template version")
        val template = database.templateDao().getTemplateVersion(s.companyId, "tpl-bilty-default", pinnedVersion)
            ?: return Result.failure(
                ErrorCode.TEMPLATE_VERSION_MISSING,
                "Template version $pinnedVersion is not on this device. A document first printed elsewhere needs one online moment to fetch that version.",
            )

        val parsed = when (val result = TemplateParser.parse(template.content_json)) {
            is ParseResult.Ok -> result.template
            is ParseResult.Refused -> return Result.failure(ErrorCode.TEMPLATE_FIELD_UNKNOWN, result.reason)
        }

        val html = HtmlRenderer.renderCopies(parsed, payloadToValues(snapshot.payload_json), copies)
        val bytes = pdfPort.render(html, "bilty-$biltyNo")
        if (bytes.isEmpty()) {
            return Result.failure(ErrorCode.EXPORT_TOO_LARGE, "The document could not be rendered — try again")
        }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ENGLISH).format(java.util.Date(System.currentTimeMillis()))
        val fileName = "Bilty-${biltyNo.replace("/", "-")}-$stamp.pdf"
        return Result.success(RenderedDocument(bytes, fileName, html.hashCode().toString(16)))
    }

    override suspend fun biltyValues(biltyNo: String): Result<Map<String, String>>? {
        val s = sessionRepository.session.first()
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(s.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(s.companyId, biltyNo)
            ?: return null
        val snapshot = database.consignmentDao().getLatestSnapshot(consignment.local_id) ?: return null
        return Result.success(payloadToValues(snapshot.payload_json))
    }

    override suspend fun copyLabels(biltyNo: String): List<String> {
        val s = sessionRepository.session.first()
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(s.companyId, biltyNo)
            ?: database.consignmentDao().getConsignmentByProvisionalNo(s.companyId, biltyNo)
            ?: return DEFAULT_COPIES
        val snapshot = database.consignmentDao().getLatestSnapshot(consignment.local_id)
            ?: return DEFAULT_COPIES
        val copies = snapshot.copy_count
        return DEFAULT_COPIES.take(copies).ifEmpty { DEFAULT_COPIES }
    }

    override fun share(document: RenderedDocument, chooserTitle: String) =
        pdfActions.share(document.pdfBytes, document.fileName, chooserTitle)

    override fun print(document: RenderedDocument) =
        pdfActions.print(document.pdfBytes, "bilty-${document.fileName.removeSuffix(".pdf")}")

    override fun saveToDownloads(document: RenderedDocument): Uri? =
        pdfActions.saveToDownloads(document.pdfBytes, document.fileName)

    companion object {
        val DEFAULT_COPIES = listOf("Copy 1 · Consignor", "Copy 2 · Consignee", "Copy 3 · Driver", "Copy 4 · Office")

        /**
         * The §8 print payload decoded to the flat value map the renderer reads by key. The
         * payload is a flat JSON object of print-ready strings; multi-row goods (a future
         * multi-article bilty) arrive as a JSON array under "items".
         */
        fun payloadToValues(payloadJson: String): Map<String, String> {
            val obj = JSONObject(payloadJson)
            val map = mutableMapOf<String, String>()
            obj.keys().forEach { key ->
                when (val value = obj.opt(key)) {
                    // JSONObject.NULL is a sentinel instance, not Kotlin null: JSON-null
                    // snapshot fields (e.g. provisionalCrossRef) decode to absent keys.
                    JSONObject.NULL -> Unit
                    is JSONObject -> map[key] = value.toString()
                    is JSONArray -> map[key] = value.toString()
                    null -> Unit
                    else -> map[key] = value.toString()
                }
            }
            return map
        }
    }
}
