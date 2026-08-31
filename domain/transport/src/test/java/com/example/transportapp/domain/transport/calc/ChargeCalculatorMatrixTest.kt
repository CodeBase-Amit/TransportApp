package com.example.transportapp.domain.transport.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Table-driven matrix: basis × min/max × rounding × GST (Phase2.md S4 test charter). */
class ChargeCalculatorMatrixTest {

    private val hamali = ChargeHeadDef("h1", "hamali", "Hamali", ChargeHeadBasis.PER_PACKAGE, 800, taxable = true, sortOrder = 1)
    private val door = ChargeHeadDef("h2", "door_delivery", "Door delivery", ChargeHeadBasis.FLAT, 15_000, taxable = true, sortOrder = 2)
    private val noHeads = emptyList<ChargeHeadDef>()
    private val forward5 = GstConfig(GstTreatment.FORWARD, 500, "Madhya Pradesh", "Madhya Pradesh")

    private fun perKg(ratePaise: Long = 450, minFreight: Long? = null, maxFreight: Long? = null, minQty: String? = null) =
        RateCandidate("r", "p", "rt", null, RateBasis.PER_KG, ratePaise, minFreight, maxFreight, minQty)

    private fun calculate(input: CalculationInput) = ChargeCalculator.calculate(input)

    private fun rateInput(candidate: RateCandidate, packages: Long = 12, actualG: Long = 780_000, gst: GstConfig = forward5) =
        CalculationInput(
            packages = packages, actualWeightG = actualG, rate = ResolvedRate(candidate, 1),
            heads = listOf(hamali, door), gst = gst, rounding = RoundingRule.NONE,
        )

    @Test
    fun `every rate basis computes its quantity correctly`() {
        assertEquals("per kg", 351_000L, calculate(rateInput(perKg())).freightPaise)
        assertEquals(
            "per tonne: 2.5 t × ₹3,200",
            800_000L,
            calculate(rateInput(RateCandidate("r", "p", "rt", null, RateBasis.PER_TONNE, 320_000), actualG = 2_500_000)).freightPaise,
        )
        assertEquals(
            "per quintal: 1.5 qtl × ₹120",
            18_000L,
            calculate(rateInput(RateCandidate("r", "p", "rt", null, RateBasis.PER_QUINTAL, 12_000), actualG = 150_000)).freightPaise,
        )
        assertEquals(
            "per package: 7 × ₹180",
            126_000L,
            calculate(rateInput(RateCandidate("r", "p", "rt", null, RateBasis.PER_PACKAGE, 18_000), packages = 7)).freightPaise,
        )
        assertEquals(
            "per trip is flat",
            90_000L,
            calculate(rateInput(RateCandidate("r", "p", "rt", null, RateBasis.PER_TRIP, 90_000), actualG = 12_000_000)).freightPaise,
        )
        assertEquals(
            "fixed for the route is flat",
            75_000L,
            calculate(rateInput(RateCandidate("r", "p", "rt", null, RateBasis.FIXED, 75_000))).freightPaise,
        )
    }

    @Test
    fun `minimum freight then maximum freight apply in that order`() {
        assertEquals("min freight floors", 35_000L, calculate(rateInput(perKg(minFreight = 35_000), actualG = 50_000)).freightPaise)
        assertEquals("max freight caps", 100_000L, calculate(rateInput(perKg(maxFreight = 100_000), actualG = 5_000_000)).freightPaise)
        assertEquals(
            "min and max together still give the computed value inside the band",
            351_000L,
            calculate(rateInput(perKg(minFreight = 35_000, maxFreight = 400_000))).freightPaise,
        )
    }

    @Test
    fun `minimum quantity floors the basis quantity`() {
        assertEquals(
            "weight basis pays for at least 500 kg",
            225_000L,
            calculate(rateInput(perKg(minQty = "500 kg"), actualG = 100_000)).let { it.chargeableWeightG to it.freightPaise }.second,
        )
        assertEquals(500_000L, calculate(rateInput(perKg(minQty = "500 kg"), actualG = 100_000)).chargeableWeightG)
        assertEquals(
            "package basis pays for at least 10 packages",
            180_000L,
            calculate(rateInput(RateCandidate("r", "p", "rt", null, RateBasis.PER_PACKAGE, 18_000, minQtyLabel = "10 pkg"), packages = 7)).freightPaise,
        )
        assertEquals(
            "flat bases ignore the minimum quantity",
            90_000L,
            calculate(rateInput(RateCandidate("r", "p", "rt", null, RateBasis.FIXED, 90_000, minQtyLabel = "500 kg"), actualG = 100_000)).freightPaise,
        )
    }

    @Test
    fun `volumetric weight participates only when enabled`() {
        val dims = PackageDims(100, 100, 100)
        val withVolumetric = CalculationInput(
            packages = 1, actualWeightG = 100_000, dims = dims, volumetricDivisor = 6000, weightStepG = 1000,
            rate = ResolvedRate(perKg(), 1), gst = forward5, rounding = RoundingRule.NONE,
        )
        assertEquals("1 m³ ÷ 6000 = 166.67 kg, stepped up", 167_000L, calculate(withVolumetric).chargeableWeightG)
        assertEquals(75_150L, calculate(withVolumetric).freightPaise)

        val disabled = withVolumetric.copy(volumetricDivisor = null)
        assertEquals("divisor off: dimensions ignored", 100_000L, calculate(disabled).chargeableWeightG)
    }

    @Test
    fun `weight step rounds up`() {
        val input = rateInput(perKg()).copy(actualWeightG = 781_000, weightStepG = 5000)
        assertEquals(785_000L, calculate(input).chargeableWeightG)
    }

    @Test
    fun `rounding rules behave`() {
        val base = rateInput(perKg(), gst = forward5.copy(rateBp = 500)) // pre-round 394380 with default heads
        val nearest = calculate(base.copy(rounding = RoundingRule.NEAREST_RUPEE))
        assertEquals(20L, nearest.roundingDeltaPaise)
        assertEquals(394_400L, nearest.grandTotalPaise)

        val up = calculate(base.copy(rounding = RoundingRule.UP_TO_RUPEE))
        assertEquals(20L, up.roundingDeltaPaise)

        val none = calculate(base.copy(rounding = RoundingRule.NONE))
        assertEquals(0L, none.roundingDeltaPaise)
        assertEquals(394_380L, none.grandTotalPaise)
        assertTrue("no rounding line when delta is zero", none.lines.none { it.headCode == "rounding" })

        val downCase = calculate(
            rateInput(perKg(ratePaise = 425)).copy(rounding = RoundingRule.NEAREST_RUPEE),
        )
        assertEquals("₹3,73,905 rounds down", -5L, downCase.roundingDeltaPaise)
        assertEquals("grand total lands on a whole rupee", 0L, Math.floorMod(downCase.grandTotalPaise, 100L))
    }

    @Test
    fun `gst treatments print what the section mandates`() {
        val reverse = calculate(
            rateInput(perKg(), gst = forward5.copy(treatment = GstTreatment.REVERSE, reverseChargeDeclaration = "Tax payable by recipient under reverse charge")),
        )
        assertEquals(0L, reverse.gst?.totalPaise)
        assertTrue(reverse.gst?.reverseChargeDeclaration ?: false)
        assertEquals("grand total carries no tax", 375_600L, reverse.grandTotalPaise)
        assertEquals(1, reverse.lines.count { it.headCode == "gst" })

        val exempt = calculate(rateInput(perKg(), gst = forward5.copy(treatment = GstTreatment.EXEMPT, exemptionReason = "Nil rated")))
        assertEquals(0L, exempt.gst?.totalPaise)
        assertEquals("Nil rated", exempt.gst?.exemptionReason)

        val interstate = calculate(rateInput(perKg(), gst = forward5.copy(placeOfSupplyState = "Maharashtra")))
        assertEquals("IGST takes the full rate", 18_780L, interstate.gst?.igstPaise)
        assertNull(interstate.gst?.cgstPaise)
        assertTrue(interstate.isInterstate)
        assertEquals("IGST (interstate)", interstate.lines.first { it.headCode == "gst" }.detail)
    }

    @Test
    fun `place of supply decides interstate - never the two stations`() {
        val subtle = GstConfig(GstTreatment.FORWARD, 500, placeOfSupplyState = "Madhya Pradesh", companyRegisteredState = "Madhya Pradesh")
        val result = calculate(rateInput(perKg(), gst = subtle))
        assertTrue("supply state equals registered state: intrastate even if the destination station lies in another state", !result.isInterstate)
    }

    @Test
    fun `discount applies before tax as its own negative line`() {
        val result = calculate(rateInput(perKg(), actualG = 780_000).copy(discountPaise = 50_000, heads = noHeads))
        assertEquals(-50_000L, result.lines.first { it.headCode == "discount" }.amountPaise)
        assertEquals(301_000L, result.taxablePaise)
        assertEquals("GST on the discounted value", 15_050L, result.gst?.totalPaise)
    }

    @Test
    fun `removed heads and manual charges behave`() {
        val withHeads = rateInput(perKg()).copy(heads = listOf(hamali))
        val kept = calculate(withHeads)
        assertEquals(1, kept.lines.count { it.headCode == "hamali" })

        val removed = calculate(withHeads.copy(removedHeadCodes = setOf("hamali")))
        assertEquals(0, removed.lines.count { it.headCode == "hamali" })
        assertEquals(351_000L, removed.taxablePaise)

        val manual = calculate(withHeads.copy(manualCharges = listOf(ManualCharge("other", "Other", 2_500, taxable = false))))
        assertEquals("non-taxable manual charge lands outside the taxable value", 2_500L, manual.nonTaxablePaise)
        assertEquals(351_000L + 9_600L, manual.taxablePaise)
    }

    @Test
    fun `no rate resolved still prices the heads`() {
        val input = CalculationInput(packages = 12, actualWeightG = 780_000, rate = null, heads = listOf(hamali), gst = forward5, rounding = RoundingRule.NONE)
        val result = calculate(input)
        assertNull(result.freightPaise)
        assertEquals(0, result.lines.count { it.headCode == "freight" })
        assertEquals(9_600L, result.taxablePaise)
    }

    @Test
    fun `percent of freight and value compute from their bases`() {
        val surcharge = ChargeHeadDef("h3", "surcharge", "Surcharge", ChargeHeadBasis.PERCENT_OF_FREIGHT, 500, taxable = true, sortOrder = 3)
        val insurance = ChargeHeadDef("h4", "insurance", "Insurance", ChargeHeadBasis.PERCENT_OF_VALUE, 50, taxable = false, sortOrder = 4)
        val result = calculate(rateInput(perKg()).copy(heads = listOf(surcharge, insurance), declaredValuePaise = 100_000))
        assertEquals("5% of ₹3,510 freight", 17_550L, result.lines.first { it.headCode == "surcharge" }.amountPaise)
        assertEquals("0.5% of ₹1,000 value", 500L, result.lines.first { it.headCode == "insurance" }.amountPaise)
        assertEquals("insurance is usually not taxable", 500L, result.nonTaxablePaise)
        assertEquals("5% of freight", result.lines.first { it.headCode == "surcharge" }.detail)
    }
}
