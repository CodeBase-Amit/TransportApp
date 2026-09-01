package com.example.transportapp.doc.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Stage 6 — the HTML renderer (Implementation.md §9.7). A pure function from template plus
 * snapshot values to one complete, self-contained HTML document: no Android, no files, no
 * PDFs, no network, no JavaScript, no external stylesheets or fonts.
 *
 * The rules that carry the design: every user-supplied value is escaped; the sheet is sized
 * in millimetres; the goods grid is a real `<table>` so the engine owns row heights and page
 * breaking (`@page` CSS is the entire pagination implementation); theme values become CSS
 * custom properties; values are read strictly by key — there is not a single business field
 * name in this file, which a source-level guard test enforces; and totals carrying a
 * whitelisted expression are recomputed from the snapshot's own values rather than trusted.
 */
object HtmlRenderer {

    private val json = Json { ignoreUnknownKeys = true }

    /** Render one sheet with no copy label — the reprint path (§9.12). */
    fun render(template: TemplateModel, values: Map<String, String>): String =
        renderCopies(template, values, emptyList())

    /**
     * Render one HTML document containing one sheet per copy label. An empty label list
     * yields a single unlabelled sheet. This is what makes a four-copy document one
     * paginated file: each sheet ends with a page break except the last.
     */
    fun renderCopies(template: TemplateModel, values: Map<String, String>, copyLabels: List<String>): String {
        val labels = if (copyLabels.isEmpty()) listOf("") else copyLabels
        val sheets = labels.joinToString("\n") { label -> sheet(template, values, label) }
        return buildString {
            append("<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\">")
            append("<title>")
            append(escape(sheetTitle(template)))
            append("</title><style>")
            append(style(template))
            append("</style></head><body>\n")
            append(sheets)
            append("\n</body></html>")
        }
    }

    private fun sheetTitle(template: TemplateModel): String =
        template.sections.firstOrNull { it.type == "title" }?.title
            ?: template.name.ifBlank { template.business.shopName }

    private fun sheet(template: TemplateModel, values: Map<String, String>, copyLabel: String): String {
        val itemRows = goodsRows(template, values)
        return buildString {
            append("<div class=\"sheet\">\n")
            if (values["voided"] == "true") append("<div class=\"watermark\">VOID</div>\n")
            template.sections.forEach { section ->
                if (section.visibleWhen != null && values[section.visibleWhen.field] != section.visibleWhen.equals) {
                    return@forEach
                }
                when (section.type) {
                    "header" -> append(header(template))
                    "title" -> {
                        append("<div class=\"titleblock\">")
                        if (copyLabel.isNotEmpty()) append("<div class=\"copyline\">")
                        if (copyLabel.isNotEmpty()) append(escape(copyLabel))
                        if (copyLabel.isNotEmpty()) append("</div>")
                        append("<div class=\"title\">")
                        append(escape(section.title ?: ""))
                        append("</div></div>\n")
                    }
                    "meta" -> append(labelValueGrid(section, values, "meta"))
                    "customer" -> append(labelValueGrid(section, values, "customer"))
                    "items" -> append(goodsTable(section, itemRows))
                    "totals" -> append(totals(section, values, itemRows))
                    "footer" -> append(labelValueGrid(section, values, "footer"))
                    "notes" -> {
                        append("<div class=\"notes\">")
                        append(escape(section.text ?: ""))
                        append("</div>\n")
                    }
                }
            }
            append("</div>\n")
        }
    }

    private fun header(template: TemplateModel): String {
        val b = template.business
        return buildString {
            append("<div class=\"header\">")
            append("<div class=\"shopname\">")
            append(escape(b.shopName))
            append("</div><div class=\"shopcontact\">")
            listOfNotNull(b.tagline, b.tel?.let { "Tel: $it" }, b.mobile, b.email, b.address, b.taxId?.let { "Tax ID: $it" })
                .forEach { append("<div>").append(escape(it)).append("</div>") }
            b.extraLines.forEach { append("<div>").append(escape(it)).append("</div>") }
            append("</div></div>\n")
        }
    }

    private fun labelValueGrid(section: Section, values: Map<String, String>, css: String): String = buildString {
        append("<div class=\"$css\"><table class=\"pairs\">")
        section.fields.forEach { field ->
            append("<tr><td class=\"label\">")
            append(escape(field.label))
            append("</td><td class=\"value\">")
            append(escape(values[field.key] ?: ""))
            append("</td></tr>")
        }
        append("</table></div>\n")
    }

    /**
     * Goods rows: a values map may carry the rows as a JSON array under the "items" key
     * (a multi-row document), otherwise any present column key promotes the scalar values
     * into a single row — the common one-row case. Padded to the template's minimum so a
     * short document still prints its ruled lines.
     */
    private fun goodsRows(template: TemplateModel, values: Map<String, String>): List<Map<String, String>> {
        val section = template.sections.first { it.type == "items" }
        val raw = values["items"]
        val parsed: List<Map<String, String>> = if (raw != null) {
            when (val element = runCatching { json.parseToJsonElement(raw) }.getOrNull()) {
                is JsonArray -> element.mapNotNull { (it as? JsonObject)?.toStringMap() }
                is JsonObject -> listOfNotNull(element.toStringMap())
                else -> emptyList()
            }
        } else {
            val present = section.columns.any { values.containsKey(it.key) }
            if (present) listOf(values) else emptyList()
        }
        val target = maxOf(section.minRows, parsed.size)
        return List(target) { i -> parsed.getOrNull(i) ?: emptyMap() }
    }

    private fun JsonObject.toStringMap(): Map<String, String> = entries.associate { (k, v) ->
        val content = (v as? JsonPrimitive)?.content
        k to (content ?: "")
    }

    private fun goodsTable(section: Section, rows: List<Map<String, String>>): String = buildString {
        append("<table class=\"goods\"><thead><tr>")
        section.columns.forEach { column ->
            append("<th style=\"width:")
            append(column.widthMm)
            append("mm\">")
            append(escape(column.label))
            append("</th>")
        }
        append("</tr></thead><tbody>")
        rows.forEach { row ->
            append("<tr>")
            section.columns.forEach { column ->
                val cell = row[column.key] ?: ""
                append("<td")
                if (cell.isNotEmpty() && cell.first().isDigit()) append(" class=\"num\"")
                append(">")
                append(escape(cell))
                append("</td>")
            }
            append("</tr>")
        }
        append("</tbody></table>\n")
    }

    private fun totals(section: Section, values: Map<String, String>, itemRows: List<Map<String, String>>): String = buildString {
        append("<table class=\"totals\">")
        section.fields.forEach { field ->
            val value = when {
                field.expression != null -> Expressions.evaluate(field.expression, itemRows) ?: (values[field.key] ?: "")
                else -> values[field.key] ?: ""
            }
            append("<tr><td class=\"label\">")
            append(escape(field.label))
            append("</td><td class=\"num\">")
            append(escape(value))
            append("</td></tr>")
        }
        append("</table>\n")
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun style(template: TemplateModel): String {
        val margin = template.paper.marginMm.coerceIn(5, 25)
        val landscape = template.paper.orientation == "landscape"
        val pageShort = 210 - 2 * margin
        val pageLong = 297 - 2 * margin
        val contentWidth = if (landscape) "$pageLong" else "$pageShort"
        return buildString {
            append("@page { size: ")
            append(if (landscape) "A4 landscape" else "A4 portrait")
            append("; margin: ").append(margin).append("mm; }\n")
            append(":root { --primary: ").append(template.theme.primaryColor)
            append("; --on-primary: ").append(template.theme.textOnPrimary).append("; }\n")
            append("body { margin: 0; font-family: ")
            append(if (template.theme.fontFamily == "serif") "Georgia, serif" else "Helvetica, Arial, sans-serif")
            append("; color: #111; }\n")
            append(".sheet { width: ").append(contentWidth).append("mm; position: relative; page-break-after: always; }\n")
            append(".sheet:last-child { page-break-after: auto; }\n")
            append(".watermark { position: absolute; top: 40%; left: 10%; font-size: 60pt; color: rgba(160,0,0,0.15); transform: rotate(-18deg); font-weight: bold; }\n")
            append(".header { display: flex; justify-content: space-between; align-items: flex-start; border-bottom: 1.2mm solid var(--primary); padding-bottom: 2mm; }\n")
            append(".shopname { font-size: 16pt; font-weight: bold; color: var(--primary); }\n")
            append(".shopcontact { text-align: right; font-size: 8pt; color: #333; }\n")
            append(".copyline { text-align: right; font-size: 8pt; color: #555; }\n")
            append(".title { text-align: center; font-size: 12pt; font-weight: bold; letter-spacing: 2px; margin: 3mm 0; }\n")
            append("table { border-collapse: collapse; width: 100%; }\n")
            append("table.pairs td { padding: 1mm 2mm; font-size: 9pt; }\n")
            append("table.pairs td.label { color: #555; width: 30%; }\n")
            append(".customer, .meta, .footer { border: 0.3mm solid var(--primary); margin: 2mm 0; padding: 1mm; }\n")
            append(".customer td.value, .meta td.value { font-weight: bold; }\n")
            append("table.goods th { background: var(--primary); color: var(--on-primary); font-size: 8pt; padding: 1.5mm; border: 0.3mm solid var(--primary); }\n")
            append("table.goods td { border: 0.3mm solid var(--primary); padding: 1.5mm; font-size: 9pt; min-height: 6mm; }\n")
            append("table.goods td.num, table.totals td.num { text-align: right; font-variant-numeric: tabular-nums; }\n")
            append("table.totals { width: 60%; margin-left: auto; margin-top: 2mm; }\n")
            append("table.totals td { padding: 1mm 2mm; font-size: 9pt; border-bottom: 0.2mm solid #ccc; }\n")
            append("table.totals tr:last-child td { font-weight: bold; font-size: 11pt; border-top: 0.4mm solid var(--primary); border-bottom: none; }\n")
            append(".notes { font-size: 8pt; color: #444; margin-top: 3mm; white-space: pre-wrap; }\n")
        }
    }
}
