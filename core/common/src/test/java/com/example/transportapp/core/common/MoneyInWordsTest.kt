package com.example.transportapp.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

/** §10.4 step 8 — the words line is computed from the rounded grand total. */
class MoneyInWordsTest {

    @Test
    fun `golden 10-6 grand total`() {
        assertEquals("Three thousand nine hundred forty four rupees only", Money.fromRupees(3944).inWords())
    }

    @Test
    fun `zero prints without doubling`() {
        assertEquals("Zero rupees only", Money.ZERO.inWords())
    }

    @Test
    fun `paise suffix prints with and`() {
        assertEquals("Three thousand nine hundred forty four rupees and fifty paise only", Money(394450).inWords())
        assertEquals("Fifty paise only", Money(50).inWords())
    }

    @Test
    fun `indian grouping lakh and crore`() {
        assertEquals("One lakh twenty three thousand four hundred fifty six rupees only", Money(12345600).inWords())
        assertEquals("Twelve crore thirty four lakh fifty six thousand seven hundred eighty nine rupees only", Money(12345678900).inWords())
    }

    @Test
    fun `crore quotient beyond ninety nine reads correctly`() {
        assertEquals("One hundred crore rupees only", Money.fromRupees(1_00_00_00_000L).inWords())
        assertEquals("One thousand two hundred thirty four crore fifty six lakh seventy eight thousand nine hundred one rupees only", Money.fromRupees(1_234_56_78_901L).inWords())
    }

    @Test
    fun `negative totals carry the minus`() {
        assertEquals("minus Twenty rupees only", Money(-2000).inWords())
    }

    @Test
    fun `ledger form prefixes rupees for the printed bilty`() {
        assertEquals("Rupees three thousand nine hundred forty four only", Money.fromRupees(3944).inWordsLedger())
        assertEquals("Rupees three thousand nine hundred forty four and fifty paise only", Money(394450).inWordsLedger())
        assertEquals("Rupees zero only", Money.ZERO.inWordsLedger())
    }
}
