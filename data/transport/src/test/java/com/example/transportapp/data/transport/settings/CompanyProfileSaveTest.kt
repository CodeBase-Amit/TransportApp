package com.example.transportapp.data.transport.settings

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.account.SettingsRepository
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.data.transport.tracking.PhotoImporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S27 (D66) regression: the T25 save persisted six of fifteen edited fields — the rest were
 * typed, draft-persisted across process death, and then silently discarded on save while
 * the letterhead preview printed the half it never received. Every edited field must land
 * in COMPANY_E and round-trip through the read path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompanyProfileSaveTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: SettingsRepository
    private val company = SeedIds.COMPANY_SHIVSHAKTI

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        val sessionFlow = MutableStateFlow(ownerSession())
        val sessions = object : SessionRepository {
            override val session = sessionFlow
            override suspend fun signIn() {}
            override suspend fun signInWithPassword(email: String, password: String) = com.example.transportapp.core.common.Result.success(Unit)
            override suspend fun updateDisplayName(name: String) {}
            override suspend fun signOut() {}
        }
        repository = SettingsRepository(
            orgDao = database.orgDao(),
            numberingDao = database.numberingDao(),
            sessionRepository = sessions,
            outboxWriter = OutboxWriter(database.outboxDao()),
            photoImporter = PhotoImporter(context),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun ownerSession() = UserSession(
        userId = "u", name = "Mahesh Patidar", email = DemoSeeder.EMAIL_DEMO_USER, role = "OWNER",
        companyId = company, companyName = "Shivshakti Roadlines", branchId = SeedIds.BRANCH_INDORE, branchName = "Indore",
    )

    @Test
    fun `the full letterhead saves and reads back`() = runTest {
        repository.saveCompanyProfile(
            companyId = company,
            name = "Shivshakti Roadlines",
            legalName = "Shivshakti Roadlines",
            address = "Transport Nagar, Indore 452003",
            gstin = "23AABCS4521M1Z9",
            pan = "AABCS4521M",
            transporterId = null,
            constitution = "Partnership",
            city = "Indore",
            pincode = "452001",
            state = "Madhya Pradesh",
            phone = "+91 98260 00000",
            altPhone = "+91 98260 00001",
            email = "accounts@shivshakti.example.in",
            website = "shivshakti.example.in",
            footerClause = "Subject to Indore jurisdiction.",
        )

        val profile = repository.companyProfile(company)!!
        assertEquals("Partnership", profile.constitution)
        assertEquals("Indore", profile.city)
        assertEquals("452001", profile.pincode)
        assertEquals("Madhya Pradesh", profile.state)
        assertEquals("+91 98260 00000", profile.phone)
        assertEquals("+91 98260 00001", profile.altPhone)
        assertEquals("accounts@shivshakti.example.in", profile.email)
        assertEquals("shivshakti.example.in", profile.website)
        assertEquals("Subject to Indore jurisdiction.", profile.footerClause)
    }
}
