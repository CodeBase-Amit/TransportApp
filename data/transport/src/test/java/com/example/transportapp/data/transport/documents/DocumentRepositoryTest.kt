package com.example.transportapp.data.transport.documents

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S13: the reprint invariant at the repository level — the seeded snapshot plus the seeded
 * PINNED template version render through the same HTML the engine's golden test pins (same
 * hash), the same document renders byte-identically twice, and a missing pinned version
 * answers TEMPLATE_VERSION_MISSING instead of silently rendering today's template (§9.12).
 * The Chromium drive itself is emulator-verified; the PDF step is faked here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: DocumentRepositoryImpl
    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val now = System.currentTimeMillis()

    private inner class FakePdfPort : PdfPort {
        var lastHtml: String = ""
        var failNext: Boolean = false

        override suspend fun render(html: String, jobName: String): ByteArray {
            if (failNext) return ByteArray(0)
            lastHtml = html
            // A marker payload that depends only on the HTML, so determinism is provable.
            return ("%PDF-fake:${html.hashCode()}").toByteArray(Charsets.US_ASCII)
        }
    }

    private val pdfPort = FakePdfPort()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = DocumentRepositoryImpl(
            database,
            object : SessionRepository {
                override val session = flowOf(
                    UserSession(
                        userId = "u", name = "Mahesh Patidar", email = DemoSeeder.EMAIL_DEMO_USER, role = "OWNER",
                        companyId = company, companyName = "Shivshakti Roadlines",
                        branchId = SeedIds.BRANCH_INDORE, branchName = "Indore",
                    ),
                )

                override suspend fun signIn() {}
        override suspend fun signInWithPassword(email: String, password: String) = com.example.transportapp.core.common.Result.success(Unit)
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signOut() {}
            },
            pdfPort,
            object : PdfActions {
                override fun share(pdfBytes: ByteArray, fileName: String, chooserTitle: String) {}
                override fun print(pdfBytes: ByteArray, jobName: String) {}
                override fun saveToDownloads(pdfBytes: ByteArray, fileName: String): android.net.Uri? = null
            },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `the four-copy bilty renders from the pinned template`() = runTest {
        val result = repository.renderBilty(
            SeedIds.BILTY_04188,
            listOf("Copy 1 · Consignor", "Copy 2 · Consignee", "Copy 3 · Driver", "Copy 4 · Office"),
        )
        val document = result.getOrNull()!!
        assertTrue("the fake bytes carry the HTML-derived marker", document.pdfBytes.isNotEmpty())
        assertEquals(4, Regex("<div class=\"sheet\">").findAll(pdfPort.lastHtml).count())
        assertTrue(pdfPort.lastHtml.contains("IND/2627/04188"))
        assertTrue(pdfPort.lastHtml.contains("3,944.00"))
        assertTrue("the sheet labels are the four §8 copies", pdfPort.lastHtml.contains("Copy 1 · Consignor"))
        assertNotNull(document.fileName.startsWith("Bilty-IND-2627-04188-"))
    }

    @Test
    fun `reprint renders byte-identically — the §9_14 determinism invariant`() = runTest {
        val labels = repository.copyLabels(SeedIds.BILTY_04188)
        assertEquals(4, labels.size)

        val first = repository.renderBilty(SeedIds.BILTY_04188, labels).getOrNull()!!
        val second = repository.renderBilty(SeedIds.BILTY_04188, labels).getOrNull()!!
        assertTrue("same snapshot + same pinned template = same document", first.pdfBytes.contentEquals(second.pdfBytes))
        assertEquals(first.htmlHash, second.htmlHash)
    }

    @Test
    fun `a missing pinned template version is refused, never rendered with today's template`() = runTest {
        // Corrupt the pinned version marker on the snapshot: the repository must refuse.
        val consignment = database.consignmentDao().getConsignmentByBiltyNo(company, SeedIds.BILTY_04188)!!
        val snapshot = database.consignmentDao().getLatestSnapshot(consignment.local_id)!!
        database.consignmentDao().upsertSnapshot(snapshot.copy(template_version = "99"))

        val result = repository.renderBilty(SeedIds.BILTY_04188, listOf("Copy 1"))
        assertTrue(result is Result.Failure)
        assertEquals(ErrorCode.TEMPLATE_VERSION_MISSING, (result as Result.Failure).code)
    }

    @Test
    fun `an unknown bilty answers MASTER_IN_USE`() = runTest {
        val result = repository.renderBilty("IND/2627/99999", listOf("Copy 1"))
        assertEquals(ErrorCode.MASTER_IN_USE, (result as Result.Failure).code)
    }

    @Test
    fun `the payload decodes to the flat value map the renderer reads by key`() = runTest {
        val values = repository.biltyValues(SeedIds.BILTY_04188)!!.getOrNull()!!
        assertEquals("IND/2627/04188", values["docNo"])
        assertEquals("3,944.00", values["grandTotal"])
        assertEquals("Deepak Steel Traders", values["consignorName"])
        assertTrue("null snapshot values decode to an empty string", values.containsKey("provisionalCrossRef").not())
    }

    @Test
    fun `three empty renders surface the typed failure, never an exception`() = runTest {
        pdfPort.failNext = true
        val result = repository.renderBilty(SeedIds.BILTY_04188, listOf("Copy 1"))
        assertTrue(result is Result.Failure)
        assertEquals(ErrorCode.EXPORT_TOO_LARGE, (result as Result.Failure).code)
        pdfPort.failNext = false
    }
}
