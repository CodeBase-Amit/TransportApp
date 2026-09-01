package com.example.transportapp.doc.engine

/**
 * The whitelisted expression evaluator (Implementation.md §9.6). Expressions are data: the
 * parser validated every expression against [TemplateParser.EXPRESSION_WHITELIST] at
 * acquisition, so evaluation here is total — no injection surface, no reflection.
 *
 * Money arithmetic is integer paise. A value string like "3,510.00" or "3944" parses to
 * paise exactly; grouping separators and a currency sign are stripped before the parse.
 * Sums format back through Indian digit grouping, matching what the printed sheet shows.
 */
object Expressions {

    /** Rows available to expressions: each element maps column key to its printed value. */
    fun evaluate(expression: String, rows: List<Map<String, String>>): String? = when (expression) {
        "count(items)" -> rows.size.toString()
        else -> {
            val target = expression.removePrefix("sum(items.").removeSuffix(")")
            if (target == expression || target.isEmpty()) {
                null
            } else {
                val total = rows.sumOf { row -> row[target]?.let { toPaise(it) } ?: 0L }
                fromPaise(total)
            }
        }
    }

    /** "3,510.00" / "₹3944" / " 187.8 " → paise. Unparsable text is worth zero. */
    fun toPaise(text: String): Long {
        val clean = text.filter { it.isDigit() || it == '.' || it == '-' }
        if (clean.isEmpty() || clean == "-" || clean == ".") return 0L
        val negative = clean.startsWith("-")
        val unsigned = if (negative) clean.drop(1) else clean
        val whole = unsigned.substringBefore('.').ifEmpty { "0" }
        val frac = unsigned.substringAfter('.', "").padEnd(2, '0').take(2)
        val paise = whole.toLongOrNull()?.times(100) ?: return 0L
        val fracPaise = frac.toLongOrNull() ?: 0L
        return if (negative) -(paise + fracPaise) else paise + fracPaise
    }

    /** Paise → "3,510.00" with Indian digit grouping (matches core:common's format). */
    fun fromPaise(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val abs = kotlin.math.abs(paise)
        val whole = abs / 100
        val frac = (abs % 100).toString().padStart(2, '0')
        return sign + groupIndian(whole) + "." + frac
    }

    private fun groupIndian(whole: Long): String {
        val digits = whole.toString()
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val groups = rest.reversed().chunked(2).map { it.reversed() }.reversed()
        return (groups + last3).joinToString(",")
    }
}
