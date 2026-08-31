package com.example.transportapp.export.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * S10 golden test: the freight register's CSV is byte-stable — the same rows always
 * produce the same file, and RFC 4180 quoting fires only when it must.
 */
class CsvWriterTest {

    private val row = BiltyRegisterRow(
        biltyNo = "IND/2627/04188",
        bookedAt = epochUtc(2026, 8, 25),
        branch = "Indore",
        consignor = "Deepak Steel Traders",
        consignee = "Nashik Hardware Mart",
        route = "Indore → Nashik",
        packages = 12,
        weightKg = 780,
        freightPaise = 351_000,
        gstPaise = 18_780,
        totalPaise = 394_400,
    )

    private fun epochUtc(year: Int, month: Int, day: Int): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month - 1, day)
        return cal.timeInMillis
    }

    @Test
    fun `the canonical row writes the golden line`() {
        val csv = CsvWriter.writeBiltyRegister(listOf(row))
        assertEquals(
            "Bilty no.,Date,Branch,Consignor,Consignee,Route,Packages,Weight (kg),Freight,GST,Total,Status\r\n" +
                "IND/2627/04188,2026-08-25,Indore,Deepak Steel Traders,Nashik Hardware Mart,Indore → Nashik,12,780,3510.00,187.80,3944.00,OK\r\n",
            csv,
        )
    }

    @Test
    fun `a field containing a comma quote or newline is quoted and doubled`() {
        val csv = CsvWriter.write(
            listOf("Name", "Note"),
            listOf(listOf("Sharma, & Sons", "said \"keep it simple\"\nsecond line")),
        )
        assertEquals(
            "Name,Note\r\n" +
                "\"Sharma, & Sons\",\"said \"\"keep it simple\"\"\nsecond line\"\r\n",
            csv,
        )
    }

    @Test
    fun `cancelled rows export flagged and money formats to two decimals`() {
        val cancelled = row.copy(biltyNo = "IND/2627/04183", cancelled = true, freightPaise = -50)
        val csv = CsvWriter.writeBiltyRegister(listOf(cancelled))
        assertTrue(csv.contains(",CANCELLED\r\n"))
        assertTrue(csv.contains("-0.50"))
    }
}
