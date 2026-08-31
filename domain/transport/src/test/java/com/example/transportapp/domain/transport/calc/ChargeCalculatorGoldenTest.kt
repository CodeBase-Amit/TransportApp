package com.example.transportapp.domain.transport.calc

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The §10.6 worked example is the fixture this engine may never disagree with:
 * Hubli to Bengaluru, per-kilogram basis at ₹4.50, minimum freight ₹350, 12 packages,
 * actual weight 780 kg, hamali ₹8 per package, door delivery ₹150, forward charge at 5%,
 * rounding to the nearest rupee → grand total ₹3,944.00.
 */
class ChargeCalculatorGoldenTest {

    private fun goldenInput() = CalculationInput(
        packages = 12,
        actualWeightG = 780_000,
        rate = ResolvedRate(
            candidate = RateCandidate(
                localId = "r1", partyId = "p", routeId = "r", goodsId = "g",
                basis = RateBasis.PER_KG, ratePaise = 450,
                minFreightPaise = 35_000,
            ),
            step = 1,
        ),
        heads = listOf(
            ChargeHeadDef("h1", "hamali", "Hamali", ChargeHeadBasis.PER_PACKAGE, 800, taxable = true, sortOrder = 1),
            ChargeHeadDef("h2", "door_delivery", "Door delivery", ChargeHeadBasis.FLAT, 15_000, taxable = true, sortOrder = 2),
        ),
        gst = GstConfig(
            treatment = GstTreatment.FORWARD,
            rateBp = 500,
            placeOfSupplyState = "Karnataka",
            companyRegisteredState = "Karnataka",
        ),
        rounding = RoundingRule.NEAREST_RUPEE,
    )

    @Test
    fun `10-6 worked example produces the exact printed figures`() {
        val result = ChargeCalculator.calculate(goldenInput())

        assertEquals(780_000L, result.chargeableWeightG)
        assertEquals("freight: 780 × 4.50", 351_000L, result.freightPaise)
        assertEquals(listOf(351_000L, 9_600L, 15_000L), result.lines.take(3).map { it.amountPaise })
        assertEquals("780 kg × 4.50", result.lines[0].detail)
        assertEquals("12 × 8.00", result.lines[1].detail)
        assertEquals("fixed", result.lines[2].detail)
        assertEquals("taxable value", 375_600L, result.taxablePaise)
        assertEquals("GST at 5%", 18_780L, result.gst?.totalPaise)
        assertEquals("intrastate splits CGST/SGST evenly", 9_390L, result.gst?.cgstPaise)
        assertEquals(9_390L, result.gst?.sgstPaise)
        assertEquals("total before rounding", 394_380L, result.taxablePaise + result.nonTaxablePaise + result.gst!!.totalPaise)
        assertEquals("rounding line", 20L, result.roundingDeltaPaise)
        assertEquals("grand total", 394_400L, result.grandTotalPaise)
        assertEquals("Three thousand nine hundred forty four rupees only", result.amountInWords)
    }

    @Test
    fun `minimum freight floors a small consignment`() {
        val result = ChargeCalculator.calculate(goldenInput().copy(actualWeightG = 50_000))

        assertEquals("50 kg × 4.50 = ₹225 floors to ₹350", 35_000L, result.freightPaise)
    }

    @Test
    fun `gst label and split follow the treatment`() {
        val result = ChargeCalculator.calculate(goldenInput())

        assertEquals("GST 5% — we pay, forward charge", result.lines.first { it.headCode == "gst" }.label)
        assertEquals("CGST + SGST (intrastate)", result.lines.first { it.headCode == "gst" }.detail)
    }
}
