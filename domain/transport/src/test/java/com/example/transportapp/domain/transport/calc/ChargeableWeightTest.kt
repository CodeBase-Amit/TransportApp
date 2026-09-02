package com.example.transportapp.domain.transport.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** §10.1 chargeable weight and the §3 minimum-quantity grammar. */
class ChargeableWeightTest {

    @Test
    fun `chargeable is actual rounded up to the weight step`() {
        assertEquals(780_000L, ChargeableWeight.chargeableG(780_000, null, 1000))
        assertEquals("781.2 kg steps up to 782", 782_000L, ChargeableWeight.chargeableG(781_200, null, 1000))
        assertEquals("5 kg step", 785_000L, ChargeableWeight.chargeableG(781_000, null, 5000))
        assertEquals("already on the step stays", 785_000L, ChargeableWeight.chargeableG(785_000, null, 5000))
    }

    @Test
    fun `volumetric weight wins when heavier`() {
        val dims = PackageDims(120, 80, 90) // 864,000 cm³ ÷ 6000 = 144 kg per package
        assertEquals(144_000L, ChargeableWeight.volumetricG(dims, 1, 6000))
        assertEquals("two packages double it", 288_000L, ChargeableWeight.volumetricG(dims, 2, 6000))

        val actual = ChargeableWeight.chargeableG(100_000, ChargeableWeight.volumetricG(dims, 1, 6000), 1000)
        assertEquals(144_000L, actual)
    }

    @Test
    fun `volumetric ceil does not lose fractions`() {
        // 1,000,000 cm³ ÷ 6000 = 166.667 kg — must ceil to 166,667 g, never floor.
        assertEquals(166_667L, ChargeableWeight.volumetricG(PackageDims(100, 100, 100), 1, 6000))
    }

    @Test
    fun `minimum quantity grammar parses the seeded labels`() {
        assertEquals(500_000L, (MinQty.parse("500 kg") as MinQty.Weight).grams)
        assertEquals(1_000_000L, (MinQty.parse("1 Ton") as MinQty.Weight).grams)
        assertEquals(3_000_000L, (MinQty.parse("3 t") as MinQty.Weight).grams)
        assertEquals(2_500_000L, (MinQty.parse("2.5 t") as MinQty.Weight).grams)
        assertEquals(100_000L, (MinQty.parse("1 qtl") as MinQty.Weight).grams)
        assertEquals(5L, (MinQty.parse("5 pkg") as MinQty.Packages).count)
        assertEquals(12L, (MinQty.parse("12 art") as MinQty.Packages).count)
        assertEquals(1_000_000L, (MinQty.parse("1,000 kg") as MinQty.Weight).grams)
    }

    @Test
    fun `unparseable minimum quantities are absent - never guessed`() {
        assertNull(MinQty.parse(null))
        assertNull(MinQty.parse(""))
        assertNull(MinQty.parse("500 kg."))
        assertNull(MinQty.parse("about half a tonne"))
        assertNull(MinQty.parse("0 kg"))
        assertNull(MinQty.parse("-5 kg"))
        assertNull(MinQty.parse("kg"))
    }

    @Test
    fun `fractional minimum quantities parse exactly - no floating point jitter (S18)`() {
        // The §14.1 regression cases: decimal strings that Double rounding would corrupt.
        assertEquals(1_500_000L, (MinQty.parse("1.5 t") as MinQty.Weight).grams)
        assertEquals(2_250_000L, (MinQty.parse("2.25 t") as MinQty.Weight).grams)
        assertEquals(500L, (MinQty.parse("0.5 kg") as MinQty.Weight).grams)
        assertEquals(1_501L, (MinQty.parse("1.501 kg") as MinQty.Weight).grams)
        assertEquals(999L, (MinQty.parse("0.999 kg") as MinQty.Weight).grams)
        assertEquals(50_000L, (MinQty.parse("0.5 qtl") as MinQty.Weight).grams)
        // Package floors are whole numbers; fractions of a package floor to nothing.
        assertEquals(5L, (MinQty.parse("5.0 pkg") as MinQty.Packages).count)
        assertNull(MinQty.parse("0.5 pkg"))
        // More than three decimals has no unit meaning at gram precision — absent.
        assertNull(MinQty.parse("1.50001 kg"))
    }
}
