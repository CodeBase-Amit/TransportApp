package com.example.transportapp.core.common

/**
 * Money is always a whole number of paise stored in a [Long]. Never Double, never Float.
 *
 * Rounding is an explicit step; currency formatting happens only at the view boundary.
 */
@JvmInline
value class Money(val paise: Long) {

    val rupees: Long get() = paise / 100
    val paisePart: Long get() = paise % 100

    operator fun plus(other: Money): Money = Money(paise + other.paise)
    operator fun minus(other: Money): Money = Money(paise - other.paise)
    operator fun times(multiplier: Long): Money = Money(paise * multiplier)
    operator fun div(divisor: Long): Money = Money(paise / divisor)

    /** The nearest-rupee rounding delta in paise (may be negative). */
    fun roundingDelta(): Money = Money(paise % 100)

    fun roundedToRupee(): Money = Money(paise - paise % 100)

    /** Indian digit grouping with two decimals: 394400 -> "3,944.00", 128490000 -> "12,84,900.00". */
    fun formatted(): String {
        val sign = if (paise < 0) "-" else ""
        val abs = kotlin.math.abs(paise)
        val whole = abs / 100
        val frac = abs % 100
        return sign + formatIndianGrouping(whole) + "." + frac.toString().padStart(2, '0')
    }

    /** Amount in words for the "rupees ... only" line. */
    fun inWords(): String = amountInWords(paise).trim() + if (paise > 0) " rupees only" else "zero rupees only"

    companion object {
        val ZERO = Money(0)
        val RUPPEE = Money(100)

        fun fromRupees(whole: Long, paisePart: Long = 0): Money = Money(whole * 100 + paisePart)
    }
}

/** Formats a non-negative integer with Indian digit grouping: 123456 -> "1,23,456". */
fun formatIndianGrouping(whole: Long): String {
    val s = whole.toString()
    if (s.length <= 3) return s
    val last3 = s.takeLast(3)
    val rest = s.dropLast(3)
    val groups = rest.reversed().chunked(2).map { it.reversed() }.reversed()
    return (groups + last3).joinToString(",")
}

private val ONES = arrayOf(
    "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
)
private val TENS = arrayOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

private fun twoDigits(n: Long): String = when {
    n < 20 -> ONES[n.toInt()]
    else -> TENS[(n / 10).toInt()].trim() + if (n % 10 != 0L) " " + ONES[(n % 10).toInt()] else ""
}

private fun threeDigits(n: Long): String {
    val hundreds = n / 100
    val rest = n % 100
    val parts = mutableListOf<String>()
    if (hundreds > 0) parts.add(ONES[hundreds.toInt()] + " hundred")
    if (rest > 0) parts.add(twoDigits(rest))
    return parts.joinToString(" ")
}

private fun amountInWords(paise: Long): String {
    val abs = kotlin.math.abs(paise)
    val whole = abs / 100
    val frac = abs % 100
    val parts = mutableListOf<String>()
    // Indian numbering: crore, lakh, thousand, then hundreds
    val crore = whole / 10_000_000
    val lakh = (whole % 10_000_000) / 100_000
    val thousand = (whole % 100_000) / 1000
    val hundred = whole % 1000
    if (crore > 0) parts.add(twoDigits(crore) + " crore")
    if (lakh > 0) parts.add(twoDigits(lakh) + " lakh")
    if (thousand > 0) parts.add(twoDigits(thousand) + " thousand")
    if (hundred > 0) parts.add(threeDigits(hundred))
    val wholeWords = parts.joinToString(" ").ifEmpty { "zero" }
    return if (frac > 0) "$wholeWords and ${twoDigits(frac)} paise" else wholeWords
}
