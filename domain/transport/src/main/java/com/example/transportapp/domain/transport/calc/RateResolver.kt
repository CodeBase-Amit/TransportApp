package com.example.transportapp.domain.transport.calc

/**
 * §3 rate resolution, pure. The booking supplies its scope (party? route? goods?); the
 * candidate rows come from the rate card store with every non-null scope dimension equal
 * to the booking's. The resolver walks the fixed five steps and picks the first hit,
 * lowest sort_order winning within a step:
 *
 * 1. party + route + goods   2. party + route   3. route + goods
 * 4. route                   5. company default (no scope at all)
 */
enum class RateBasis { PER_KG, PER_TONNE, PER_QUINTAL, PER_PACKAGE, PER_TRIP, FIXED }

data class RateCandidate(
    val localId: String,
    val partyId: String?,
    val routeId: String?,
    val goodsId: String?,
    val basis: RateBasis,
    val ratePaise: Long,
    val minFreightPaise: Long? = null,
    val maxFreightPaise: Long? = null,
    val minQtyLabel: String? = null,
    val sortOrder: Int = 0,
)

data class ResolvedRate(
    val candidate: RateCandidate,
    /** 1–5, the §3 step that won; drives the T5 source note and the fallback banner. */
    val step: Int,
)

object RateResolver {

    fun resolve(candidates: List<RateCandidate>, partyId: String?, routeId: String?, goodsId: String?): ResolvedRate? {
        val steps = listOf(
            Triple(partyId, routeId, goodsId),
            Triple(partyId, routeId, null),
            Triple(null, routeId, goodsId),
            Triple(null, routeId, null),
            Triple(null, null, null),
        )
        for ((party, route, goods) in steps) {
            if (party != null && route == null) continue // party+goods without a route is not a §3 step
            candidates
                .filter { it.partyId == party && it.routeId == route && it.goodsId == goods }
                .minWithOrNull(compareBy({ it.sortOrder }, { it.localId }))
                ?.let { return ResolvedRate(it, step = stepOf(it)) }
        }
        return null
    }

    /**
     * The step is the winning row's own scope shape, not the walk index — a booking with
     * no route degenerates the walk but the company default is still step 5, and the
     * fallback banner and source note both depend on that.
     */
    private fun stepOf(candidate: RateCandidate): Int = when {
        candidate.partyId != null && candidate.routeId != null && candidate.goodsId != null -> 1
        candidate.partyId != null && candidate.routeId != null -> 2
        candidate.partyId == null && candidate.routeId != null && candidate.goodsId != null -> 3
        candidate.partyId == null && candidate.routeId != null -> 4
        else -> 5
    }
}

/**
 * The rate row's minimum quantity ("500 kg", "1 Ton", "5 pkg") as a pricing floor on the
 * basis quantity: weight bases pay for at least this weight, per-package rates for at
 * least this many packages, flat rates ignore it. Parsed from the T20 label with a strict
 * grammar; anything unparseable is treated as absent rather than guessed.
 */
sealed interface MinQty {
    data class Weight(val grams: Long) : MinQty
    data class Packages(val count: Long) : MinQty

    companion object {
        fun parse(label: String?): MinQty? {
            val text = label?.trim()?.lowercase()?.replace(",", "") ?: return null
            if (text.isEmpty()) return null
            val match = Regex("^(\\d+(?:\\.\\d+)?)\\s*(kg|kgs|t|ton|tons|tonne|tonnes|qtl|quintal|pkg|pkgs|art|arts)$").find(text) ?: return null
            // §14.1: no floating point on the money/weight path — the decimal string is
            // split and scaled with integer arithmetic ("1.5" → 1*1000 + 500*1 for kg).
            val whole = match.groupValues[1].takeWhile { it != '.' }
            val frac = match.groupValues[1].dropWhile { it != '.' }.drop(1).take(3).padEnd(3, '0')
            if (whole.isEmpty() || whole.toLongOrNull() == null || frac.toLongOrNull() == null) return null
            if (match.groupValues[1].contains('.') && match.groupValues[1].dropWhile { it != '.' }.drop(1).length > 3) return null
            val milli = whole.toLong() * 1000L + frac.toLong()
            if (milli <= 0L) return null
            return when (match.groupValues[2]) {
                "kg", "kgs" -> Weight(milli)                                  // gram-precise
                "t", "ton", "tons", "tonne", "tonnes" -> Weight(milli * 1000)  // t → g: ×10⁶ from milli-units
                "qtl", "quintal" -> Weight(milli * 100)
                // Packages are discrete: a fractional package floor is a guess, not a number.
                else -> if (milli % 1000L == 0L) Packages(milli / 1000) else null
            }
        }
    }
}
