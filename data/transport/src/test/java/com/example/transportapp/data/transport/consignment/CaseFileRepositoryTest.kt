package com.example.transportapp.data.transport.consignment

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S6: the case file assembles one consignment's whole story from local storage (§7.2, §8,
 * Design T8) — the 04188 fixture checks the header, the live timeline, the documents, the
 * money position and the record lines.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CaseFileRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: CaseFileRepositoryImpl

    private val company = SeedIds.COMPANY_SHIVSHAKTI

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = CaseFileRepositoryImpl(database.consignmentDao(), database.mastersDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val now = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000

    @Test
    fun `04188 case file assembles the whole fixture`() = runTest {
        val case = repository.caseFile(company, "IND/2627/04188", "Indore", now)!!

        assertEquals("IND/2627/04188", case.biltyNo)
        assertEquals(ConsignmentStatus.IN_TRANSIT, case.status)
        assertEquals(PaymentMode.TOPAY, case.paymentMode)
        assertEquals("Indore", case.fromStation)
        assertEquals("Nashik", case.toStation)
        assertEquals("585 km distance from the route master", 585, case.distanceKm)
        assertEquals(12L, case.packages)
        assertEquals(780L, case.chargeableKg)
        assertTrue("booked text names the clerk", case.bookedText.contains("by Mahesh Patidar"))

        // The timeline: three real events oldest-first, then the unreached Arrived tick.
        assertEquals(listOf("Booked", "Loaded", "In transit", "Arrived"), case.events.map { it.type })
        assertTrue("only the final tick is unreached", case.events.dropLast(1).none { it.unreached })
        assertTrue(case.events.last().unreached)
        assertTrue("the unreached tick expects a date", case.events.last().atText!!.startsWith("expected "))
        assertEquals("Nashik", case.events.last().location)
    }

    @Test
    fun `the money position sums the stored charge lines`() = runTest {
        val case = repository.caseFile(company, "IND/2627/04188", "Indore", now)!!

        assertEquals(listOf("Freight", "Charges", "GST", "Total to collect"), case.money.map { it.label })
        assertEquals("freight from the frozen lines", 351_000L, case.money[0].amountPaise)
        assertEquals("charges = hamali 96 + door 150", 24_600L, case.money[1].amountPaise)
        assertEquals(18_780L, case.money[2].amountPaise)
        assertEquals("total is strong", 394_400L, case.money[3].amountPaise)
        assertTrue(case.money[3].strong)

        assertEquals(
            "To Pay — collect 3,944.00 at Nashik before handing over the goods.",
            case.toPayCallout,
        )
    }

    @Test
    fun `a paid consignment has no to-pay callout`() = runTest {
        val case = repository.caseFile(company, "IND/2627/04186", "Indore", now)!!
        assertEquals(PaymentMode.PAID, case.paymentMode)
        assertNull(case.toPayCallout)
        assertEquals("delivered consignments end at the real Delivered event", listOf("Booked", "Loaded", "In transit", "Arrived", "Delivered"), case.events.map { it.type })
        assertTrue("no unreached tick after delivery", case.events.none { it.unreached })
    }

    @Test
    fun `documents describe what exists and what does not`() = runTest {
        val case = repository.caseFile(company, "IND/2627/04188", "Indore", now)!!

        assertEquals(listOf("Bilty", "Loading challan", "Freight bill", "POD"), case.documents.map { it.title })
        assertEquals("IND/2627/04188", case.documents[0].number)
        assertEquals("4 copies", case.documents[0].trailing)
        assertEquals("no challan event is seeded yet", "Not issued yet", case.documents[1].trailing)
        assertEquals("Not raised yet", case.documents[2].trailing)
        assertEquals("Pending delivery", case.documents[3].trailing)
    }

    @Test
    fun `record lines carry the snapshot provenance`() = runTest {
        val case = repository.caseFile(company, "IND/2627/04188", "Indore", now)!!

        assertTrue(case.recordLines[0].startsWith("Booked at Indore by Mahesh Patidar"))
        assertTrue(case.recordLines[1].startsWith("Snapshot v1 · template tpl-bilty-default"))
        assertTrue(case.recordLines[1].endsWith("reprints will match the copies already issued."))
    }

    @Test
    fun `unknown bilty yields null`() = runTest {
        assertNull(repository.caseFile(company, "IND/2627/99999", "Indore", now))
    }
}
