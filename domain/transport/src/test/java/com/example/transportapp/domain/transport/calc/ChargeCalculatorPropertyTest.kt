package com.example.transportapp.domain.transport.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The invariant that makes the printed arithmetic add up: the grand total is always the
 * exact sum of the printed lines in paise, whatever the inputs (§10.4 step 7 exists so
 * this property survives rounding).
 */
class ChargeCalculatorPropertyTest {

    @Test
    fun `grand total always equals the sum of the lines`() {
        val random = Random(4242)
        val bases = RateBasis.entries
        val gstConfigs = listOf(
            GstConfig(GstTreatment.FORWARD, 500, "Madhya Pradesh", "Madhya Pradesh"),
            GstConfig(GstTreatment.FORWARD, 1200, "Maharashtra", "Madhya Pradesh"),
            GstConfig(GstTreatment.FORWARD, 0, "Delhi", "Delhi"),
            GstConfig(GstTreatment.REVERSE, 500, "Rajasthan", "Madhya Pradesh"),
            GstConfig(GstTreatment.EXEMPT, 500, null, "Madhya Pradesh", exemptionReason = "Nil rated"),
        )

        repeat(300) { i ->
            val candidate = RateCandidate(
                localId = "r$i",
                partyId = "p", routeId = "rt", goodsId = if (i % 2 == 0) "g" else null,
                basis = bases[random.nextInt(bases.size)],
                ratePaise = random.nextLong(100, 500_000),
                minFreightPaise = if (i % 3 == 0) random.nextLong(0, 100_000) else null,
                maxFreightPaise = if (i % 4 == 0) random.nextLong(100_000, 2_000_000) else null,
                minQtyLabel = listOf(null, "500 kg", "1 Ton", "5 pkg", "2 t")[random.nextInt(5)],
                sortOrder = 0,
            )
            val heads = listOf(
                ChargeHeadDef("h1", "hamali", "Hamali", ChargeHeadBasis.PER_PACKAGE, 800, taxable = true, sortOrder = 1),
                ChargeHeadDef("h2", "insurance", "Insurance", ChargeHeadBasis.PERCENT_OF_VALUE, 50, taxable = false, sortOrder = 2),
            )
            val input = CalculationInput(
                packages = random.nextLong(0, 200),
                actualWeightG = random.nextLong(0, 20_000_000),
                weightStepG = if (i % 5 == 0) 5000 else 1000,
                rate = ResolvedRate(candidate, 1),
                declaredValuePaise = random.nextLong(0, 5_000_000),
                heads = heads,
                removedHeadCodes = if (i % 7 == 0) setOf("hamali") else emptySet(),
                discountPaise = if (i % 6 == 0) random.nextLong(0, 50_000) else 0,
                gst = gstConfigs[random.nextInt(gstConfigs.size)],
                rounding = RoundingRule.entries[i % RoundingRule.entries.size],
                billableDays = if (i % 9 == 0) random.nextLong(0, 10) else 0,
            )

            val result = ChargeCalculator.calculate(input)

            assertEquals(
                "iteration $i: grand total must equal the sum of the printed lines",
                result.grandTotalPaise,
                result.lines.sumOf { it.amountPaise },
            )
            assertEquals(
                "iteration $i: taxable value must equal the taxable lines",
                result.taxablePaise,
                result.lines.filter { it.taxable }.sumOf { it.amountPaise },
            )
            assertEquals(
                "iteration $i: pre-rounding total must equal taxable + non-taxable + gst",
                result.grandTotalPaise - result.roundingDeltaPaise,
                result.taxablePaise + result.nonTaxablePaise + (result.gst?.totalPaise ?: 0),
            )
            assertTrue("iteration $i: words must render", result.amountInWords.isNotEmpty())
            if (input.rounding != RoundingRule.NONE) {
                assertEquals(
                    "iteration $i: a rounded total lands on a whole rupee",
                    0L,
                    result.grandTotalPaise % 100,
                )
            }
        }
    }
}
