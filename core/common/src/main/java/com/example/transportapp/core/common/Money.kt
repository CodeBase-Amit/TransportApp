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

    /** Amount in words for the "rupees ... only" line (§10.4 step 8). */
    fun inWords(): String {
        val abs = kotlin.math.abs(paise)
        val whole = abs / 100
        val frac = abs % 100
        val parts = mutableListOf<String>()
        if (whole > 0) parts.add(capitalizeFirst(indianWords(whole)) + " rupees")
        if (frac > 0) parts.add(twoDigits(frac) + " paise")
        if (parts.isEmpty()) return "Zero rupees only"
        val core = capitalizeFirst(parts.joinToString(" and "))
        return (if (paise < 0) "minus " else "") + core + " only"
    }

    /**
     * The printed-lettersheet form ("Rupees three thousand nine hundred forty four only")
     * that the bilty's words block uses.
     */
    fun inWordsLedger(): String {
        val abs = kotlin.math.abs(paise)
        val whole = abs / 100
        val frac = abs % 100
        val parts = mutableListOf<String>()
        if (whole > 0) parts.add(indianWords(whole))
        if (frac > 0) parts.add(twoDigits(frac) + " paise")
        if (parts.isEmpty()) return "Rupees zero only"
        return (if (paise < 0) "minus " else "") + "Rupees " + parts.joinToString(" and ") + " only"
    }

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

private fun capitalizeFirst(text: String): String =
    if (text.isEmpty()) text else text[0].uppercase() + text.substring(1)

private fun joinWords(head: String, tail: String): String = if (tail.isEmpty()) head else "$head $tail"

/**
 * Indian words: crore → lakh → thousand → hundreds, the quotient read the same way so
 * values beyond 99 crore still print correctly ("one thousand two hundred thirty four crore").
 */
private fun indianWords(n: Long): String = when {
    n <= 0L -> ""
    n >= 10_000_000L -> joinWords(indianWords(n / 10_000_000L) + " crore", indianWords(n % 10_000_000L))
    n >= 100_000L -> joinWords(twoDigits(n / 100_000L) + " lakh", indianWords(n % 100_000L))
    n >= 1000L -> joinWords(twoDigits(n / 1000L) + " thousand", threeDigits(n % 1000L))
    else -> threeDigits(n)
}
