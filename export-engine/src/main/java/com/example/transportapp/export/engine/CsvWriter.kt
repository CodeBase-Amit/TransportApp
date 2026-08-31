package com.example.transportapp.export.engine

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * RFC 4180 CSV: quote a field only when it must be quoted (comma, quote, newline), double
 * the quotes inside, and always CRLF line endings. Deterministic output — the same rows
 * always produce byte-identical CSV, which is what the golden test pins.
 */
object CsvWriter {

    private const val CRLF = "\r\n"

    fun write(columns: List<String>, rows: List<List<String>>): String = buildString {
        append(join(columns))
        append(CRLF)
        rows.forEach {
            append(join(it))
            append(CRLF)
        }
    }

    fun writeBiltyRegister(rows: List<BiltyRegisterRow>, dateFormat: SimpleDateFormat = ISO_DATE): String =
        write(
            BILTY_REGISTER_COLUMNS,
            rows.map { row ->
                listOf(
                    row.biltyNo,
                    dateFormat.format(row.bookedAt),
                    row.branch,
                    row.consignor,
                    row.consignee,
                    row.route,
                    row.packages.toString(),
                    row.weightKg.toString(),
                    paise(row.freightPaise),
                    paise(row.gstPaise),
                    paise(row.totalPaise),
                    if (row.cancelled) "CANCELLED" else "OK",
                )
            },
        )

    /** `123456` paise → "1234.56". */
    fun paise(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val abs = kotlin.math.abs(paise)
        return sign + (abs / 100).toString() + "." + (abs % 100).toString().padStart(2, '0')
    }

    private fun join(fields: List<String>): String =
        fields.joinToString(",") { escape(it) }

    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }

    /** Export dates print as 2026-08-30 — locale-free, timezone-free, spreadsheet-friendly. */
    val ISO_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}
