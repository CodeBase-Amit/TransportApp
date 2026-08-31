package com.example.transportapp.domain.transport.calc

import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.formatIndianGrouping

/**
 * §10.4 freight and charge calculation — a fixed sequence, tested as a golden flow, because
 * reordering it silently changes every total. Pure functions over integer paise and grams;
 * the backend recomputation shares or ports this file so a server can never disagree with
 * the printed bilty.
 *
 * GST treatments (§10.5) are recorded and printed here, never filed: the rate itself is an
 * input ([GstConfig.rateBp]) that company settings supply with a dated history — no rate is
 * hardcoded in this engine. Interstate is decided by comparing the place of supply against
 * the company's registered state, never by comparing the two stations.
 */
enum class ChargeHeadBasis { FLAT, PER_PACKAGE, PER_KG, PERCENT_OF_FREIGHT, PERCENT_OF_VALUE, PER_DAY }

data class ChargeHeadDef(
    val localId: String,
    val code: String,
    val label: String,
    val basis: ChargeHeadBasis,
    /** Paise for FLAT/PER_PACKAGE/PER_KG; percent×100 (500 = 5.00%) for percent bases. */
    val defaultValue: Long,
    val taxable: Boolean,
    val sortOrder: Int,
)

enum class GstTreatment { FORWARD, REVERSE, EXEMPT }

data class GstConfig(
    val treatment: GstTreatment,
    /** Rate in hundredths of a percent: 500 = 5.00%. From company settings, dated (§10.5). */
    val rateBp: Int,
    /** The stored place of supply (§10.5) — a consignment field, never derived from stations. */
    val placeOfSupplyState: String?,
    val companyRegisteredState: String?,
    val exemptionReason: String? = null,
    /** The mandated reverse-charge wording comes from settings; the engine never hardcodes it. */
    val reverseChargeDeclaration: String? = null,
)

data class ManualCharge(
    val code: String?,
    val label: String,
    val amountPaise: Long,
    val taxable: Boolean,
)

data class CalculationInput(
    val packages: Long,
    val actualWeightG: Long,
    val dims: PackageDims? = null,
    /** Null disables volumetric weight for this company (§10.1). */
    val volumetricDivisor: Long? = null,
    val weightStepG: Long = 1000,
    val rate: ResolvedRate?,
    val declaredValuePaise: Long = 0,
    /** Auto-apply heads in the company's configured head order (§10.4 step 3). */
    val heads: List<ChargeHeadDef> = emptyList(),
    /** Charge rows the clerk removed on the form; they rejoin nothing. */
    val removedHeadCodes: Set<String> = emptySet(),
    val manualCharges: List<ManualCharge> = emptyList(),
    /** §10.4 step 4 — applied before tax, as its own negative line. */
    val discountPaise: Long = 0,
    val gst: GstConfig,
    val rounding: RoundingRule,
    /** Days beyond the free days, for per-day heads such as demurrage. */
    val billableDays: Long = 0,
)

enum class RoundingRule { NONE, NEAREST_RUPEE, UP_TO_RUPEE }

data class GstBreakdown(
    val totalPaise: Long,
    val cgstPaise: Long?,
    val sgstPaise: Long?,
    val igstPaise: Long?,
    val interstate: Boolean,
    val reverseChargeDeclaration: Boolean,
    val exemptionReason: String?,
)

data class CalculatedLine(
    val headCode: String?,
    val label: String,
    val detail: String,
    val amountPaise: Long,
    val taxable: Boolean,
    val computed: Boolean,
    val removable: Boolean,
)

data class CalculationResult(
    val chargeableWeightG: Long,
    val freightPaise: Long?,
    /** Freight, heads, manual charges, discount, GST and rounding — in §10.4 order. */
    val lines: List<CalculatedLine>,
    val taxablePaise: Long,
    val nonTaxablePaise: Long,
    val gst: GstBreakdown?,
    val grandTotalPaise: Long,
    val roundingDeltaPaise: Long,
    val amountInWords: String,
    val isInterstate: Boolean,
)

object ChargeCalculator {

    fun calculate(input: CalculationInput): CalculationResult {
        require(input.packages >= 0) { "packages must be non-negative" }
        require(input.actualWeightG >= 0) { "weight must be non-negative" }

        val rate = input.rate?.candidate

        // 1. Resolve chargeable weight (§10.1), then apply the rate row's minimum quantity.
        val volumetricG = if (input.dims != null && input.volumetricDivisor != null && input.packages > 0) {
            ChargeableWeight.volumetricG(input.dims, input.packages, input.volumetricDivisor)
        } else {
            null
        }
        val baseChargeableG = ChargeableWeight.chargeableG(input.actualWeightG, volumetricG, input.weightStepG)
        val minQty = MinQty.parse(rate?.minQtyLabel)
        val isWeightBasis = rate == null || rate.basis in listOf(RateBasis.PER_KG, RateBasis.PER_TONNE, RateBasis.PER_QUINTAL)
        val chargeableG = when {
            rate == null -> baseChargeableG
            isWeightBasis && minQty is MinQty.Weight -> maxOf(baseChargeableG, minQty.grams)
            else -> baseChargeableG
        }
        val pricedPackages = when {
            rate != null && rate.basis == RateBasis.PER_PACKAGE && minQty is MinQty.Packages -> maxOf(input.packages, minQty.count)
            else -> input.packages
        }

        val lines = mutableListOf<CalculatedLine>()

        // 2. Freight from the rate basis, then the minimum, then the maximum (§10.2).
        val freight = rate?.let { freightPaise(it, chargeableG, pricedPackages) }
        if (rate != null && freight != null) {
            lines += CalculatedLine(
                headCode = "freight",
                label = "Freight",
                detail = freightDetail(rate, chargeableG, pricedPackages),
                amountPaise = freight,
                taxable = true,
                computed = true,
                removable = false,
            )
        }

        // 3. Each applicable charge head in the company's configured head order (§10.4).
        input.heads
            .filter { it.code != "freight" && it.code != "discount" && it.code !in input.removedHeadCodes }
            .sortedBy { it.sortOrder }
            .forEach { head ->
                val amount = headAmount(head, chargeableG, pricedPackages, freight, input)
                if (amount > 0) {
                    lines += CalculatedLine(
                        headCode = head.code,
                        label = head.label,
                        detail = headDetail(head, chargeableG, pricedPackages, input),
                        amountPaise = amount,
                        taxable = head.taxable,
                        computed = true,
                        removable = true,
                    )
                }
            }

        input.manualCharges.forEach { charge ->
            lines += CalculatedLine(
                headCode = charge.code,
                label = charge.label,
                detail = "",
                amountPaise = charge.amountPaise,
                taxable = charge.taxable,
                computed = false,
                removable = true,
            )
        }

        // 4. The discount, applied before tax (§10.4 step 4).
        if (input.discountPaise > 0) {
            lines += CalculatedLine(
                headCode = "discount",
                label = "Discount",
                detail = "",
                amountPaise = -input.discountPaise,
                taxable = true,
                computed = false,
                removable = true,
            )
        }

        // 5. Taxable and non-taxable sums are kept apart (§10.4 step 5).
        val taxablePaise = lines.filter { it.taxable }.sumOf { it.amountPaise }
        val nonTaxablePaise = lines.filter { !it.taxable }.sumOf { it.amountPaise }

        // 6. GST on the taxable value only (§10.4 step 6, §10.5).
        val isInterstate = isInterstate(input.gst)
        val gstBreakdown: GstBreakdown?
        when (input.gst.treatment) {
            GstTreatment.FORWARD -> {
                val gstTotal = if (taxablePaise > 0) mulDivHalfUp(taxablePaise, input.gst.rateBp.toLong(), 10_000) else 0L
                gstBreakdown = if (isInterstate) {
                    GstBreakdown(gstTotal, null, null, gstTotal, true, false, null)
                } else {
                    val cgst = gstTotal / 2
                    GstBreakdown(gstTotal, cgst, gstTotal - cgst, null, false, false, null)
                }
                if (gstTotal > 0) {
                    lines += CalculatedLine(
                        headCode = "gst",
                        label = "GST ${percentLabel(input.gst.rateBp.toLong())}% — we pay, forward charge",
                        detail = if (isInterstate) "IGST (interstate)" else "CGST + SGST (intrastate)",
                        amountPaise = gstTotal,
                        taxable = false,
                        computed = true,
                        removable = false,
                    )
                }
            }
            GstTreatment.REVERSE -> {
                gstBreakdown = GstBreakdown(0, null, null, null, isInterstate, true, null)
                lines += CalculatedLine(
                    headCode = "gst",
                    label = "GST — reverse charge",
                    detail = input.gst.reverseChargeDeclaration ?: "Tax is payable by the recipient",
                    amountPaise = 0,
                    taxable = false,
                    computed = true,
                    removable = false,
                )
            }
            GstTreatment.EXEMPT -> {
                gstBreakdown = GstBreakdown(0, null, null, null, isInterstate, false, input.gst.exemptionReason)
                lines += CalculatedLine(
                    headCode = "gst",
                    label = "GST — exempt",
                    detail = input.gst.exemptionReason ?: "Nil rated",
                    amountPaise = 0,
                    taxable = false,
                    computed = true,
                    removable = false,
                )
            }
        }

        // 7. Round the grand total per the company rule; the delta prints as its own line (§10.4).
        val beforeRounding = taxablePaise + nonTaxablePaise + (gstBreakdown?.totalPaise ?: 0)
        val (grandTotal, roundingDelta) = applyRounding(beforeRounding, input.rounding)
        if (roundingDelta != 0L) {
            lines += CalculatedLine(
                headCode = "rounding",
                label = "Rounding",
                detail = "",
                amountPaise = roundingDelta,
                taxable = false,
                computed = true,
                removable = false,
            )
        }

        // 8. Amount in words from the rounded grand total (§10.4 step 8).
        return CalculationResult(
            chargeableWeightG = chargeableG,
            freightPaise = freight,
            lines = lines,
            taxablePaise = taxablePaise,
            nonTaxablePaise = nonTaxablePaise,
            gst = gstBreakdown,
            grandTotalPaise = grandTotal,
            roundingDeltaPaise = roundingDelta,
            amountInWords = Money(grandTotal).inWords(),
            isInterstate = isInterstate,
        )
    }

    /** Half-up a×b÷d for non-negative a, b and positive d; values stay far inside Long. */
    private fun mulDivHalfUp(a: Long, b: Long, d: Long): Long = (a * b + d / 2) / d

    private fun freightPaise(rate: RateCandidate, chargeableG: Long, packages: Long): Long {
        val computed = when (rate.basis) {
            RateBasis.PER_KG -> mulDivHalfUp(chargeableG, rate.ratePaise, 1000)
            RateBasis.PER_TONNE -> mulDivHalfUp(chargeableG, rate.ratePaise, 1_000_000)
            RateBasis.PER_QUINTAL -> mulDivHalfUp(chargeableG, rate.ratePaise, 100_000)
            RateBasis.PER_PACKAGE -> packages * rate.ratePaise
            RateBasis.PER_TRIP, RateBasis.FIXED -> rate.ratePaise
        }
        var freight = computed
        rate.minFreightPaise?.let { if (freight < it) freight = it }
        rate.maxFreightPaise?.let { if (freight > it) freight = it }
        return freight
    }

    private fun headAmount(
        head: ChargeHeadDef,
        chargeableG: Long,
        packages: Long,
        freightPaise: Long?,
        input: CalculationInput,
    ): Long = when (head.basis) {
        ChargeHeadBasis.FLAT -> head.defaultValue
        ChargeHeadBasis.PER_PACKAGE -> packages * head.defaultValue
        ChargeHeadBasis.PER_KG -> mulDivHalfUp(chargeableG, head.defaultValue, 1000)
        ChargeHeadBasis.PERCENT_OF_FREIGHT -> mulDivHalfUp(freightPaise ?: 0L, head.defaultValue, 10_000)
        ChargeHeadBasis.PERCENT_OF_VALUE -> mulDivHalfUp(input.declaredValuePaise, head.defaultValue, 10_000)
        ChargeHeadBasis.PER_DAY -> input.billableDays * head.defaultValue
    }

    private fun freightDetail(rate: RateCandidate, chargeableG: Long, packages: Long): String = when (rate.basis) {
        RateBasis.PER_KG -> "${kgLabel(chargeableG)} × ${Money(rate.ratePaise).formatted()}"
        RateBasis.PER_TONNE -> "${tonneLabel(chargeableG)} × ${Money(rate.ratePaise).formatted()}"
        RateBasis.PER_QUINTAL -> "${quintalLabel(chargeableG)} × ${Money(rate.ratePaise).formatted()}"
        RateBasis.PER_PACKAGE -> "$packages × ${Money(rate.ratePaise).formatted()}"
        RateBasis.PER_TRIP, RateBasis.FIXED -> "fixed"
    }

    private fun headDetail(head: ChargeHeadDef, chargeableG: Long, packages: Long, input: CalculationInput): String =
        when (head.basis) {
            ChargeHeadBasis.FLAT -> "fixed"
            ChargeHeadBasis.PER_PACKAGE -> "$packages × ${Money(head.defaultValue).formatted()}"
            ChargeHeadBasis.PER_KG -> "${kgLabel(chargeableG)} × ${Money(head.defaultValue).formatted()}"
            ChargeHeadBasis.PERCENT_OF_FREIGHT -> "${percentLabel(head.defaultValue)}% of freight"
            ChargeHeadBasis.PERCENT_OF_VALUE -> "${percentLabel(head.defaultValue)}% of goods value"
            ChargeHeadBasis.PER_DAY -> "${input.billableDays} days × ${Money(head.defaultValue).formatted()}"
        }

    /**
     * §10.5: interstate is place of supply against the company's registered state — never
     * the two stations. Unknown states are treated as intrastate (the conservative GTA
     * default; a consignment cannot be booked without a place of supply in S5 anyway).
     */
    private fun isInterstate(gst: GstConfig): Boolean {
        val supply = gst.placeOfSupplyState?.trim()?.lowercase() ?: return false
        val registered = gst.companyRegisteredState?.trim()?.lowercase() ?: return false
        return supply.isNotEmpty() && registered.isNotEmpty() && supply != registered
    }

    private fun applyRounding(before: Long, rule: RoundingRule): Pair<Long, Long> = when (rule) {
        RoundingRule.NONE -> before to 0L
        RoundingRule.NEAREST_RUPEE -> {
            val remainder = Math.floorMod(before, 100L)
            val delta = if (remainder >= 50) 100L - remainder else -remainder
            (before + delta) to delta
        }
        RoundingRule.UP_TO_RUPEE -> {
            val remainder = Math.floorMod(before, 100L)
            val delta = (100L - remainder) % 100
            (before + delta) to delta
        }
    }

    /** 500 → "5", 2850 → "28.5", 50 → "0.5" — integer-only percent formatting. */
    private fun percentLabel(hundredths: Long): String {
        val whole = hundredths / 100
        val frac = hundredths % 100
        return when {
            frac == 0L -> whole.toString()
            frac % 10 == 0L -> "$whole.${frac / 10}"
            else -> "$whole.$frac"
        }
    }

    private fun kgLabel(grams: Long): String = when {
        grams % 1000 == 0L -> formatIndianGrouping(grams / 1000) + " kg"
        else -> "${grams / 1000}.${(grams % 1000) * 10 / 1000} kg"
    }

    private fun tonneLabel(grams: Long): String = when {
        grams % 1_000_000 == 0L -> formatIndianGrouping(grams / 1_000_000) + " t"
        else -> "${grams / 1_000_000}.${(grams % 1_000_000) * 10 / 1_000_000} t"
    }

    private fun quintalLabel(grams: Long): String = when {
        grams % 100_000 == 0L -> formatIndianGrouping(grams / 100_000) + " qtl"
        else -> "${grams / 100_000}.${(grams % 100_000) * 10 / 100_000} qtl"
    }
}
