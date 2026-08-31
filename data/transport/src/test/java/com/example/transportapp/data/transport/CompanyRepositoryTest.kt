package com.example.transportapp.data.transport

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.outbox.OutboxState
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.core.datastore.context.ActiveContextStore
import com.example.transportapp.core.datastore.session.SessionStore
import com.example.transportapp.data.transport.company.CompanyRepository
import com.example.transportapp.data.transport.company.CompanyRepositoryImpl
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.domain.transport.org.MembershipStatus
import com.example.transportapp.domain.transport.org.RegisterCompanyRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S2 repository tests (Phase2.md §7): in-memory Room + the real DataStores.
 * Covers pick/switch, invitation accept/decline, and register-company with
 * outbox prerequisites.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompanyRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: CompanyRepository
    private lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sessionStore = SessionStore(context)
        repository = CompanyRepositoryImpl(
            database = database,
            orgDao = database.orgDao(),
            outboxWriter = OutboxWriter(database.outboxDao()),
            sessionStore = sessionStore,
            activeContextStore = ActiveContextStore(context),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeded org shows two companies and one invitation for the demo user`() = runTest {
        DemoSeeder(database).seedIfNeeded()

        val memberships = repository.observeMembershipsForUser(DemoSeeder.EMAIL_DEMO_USER).first()
        assertEquals(3, memberships.size)
        assertEquals(2, memberships.count { it.status == MembershipStatus.ACTIVE })
        assertEquals(1, memberships.count { it.status == MembershipStatus.INVITED })

        val companies = repository.observeCompanies().first()
        assertEquals(listOf("Bharat Cargo Carriers", "Malwa Goods Transport", "Shivshakti Roadlines"), companies.map { it.name })
    }

    @Test
    fun `selecting a company and branch persists the active context`() = runTest {
        DemoSeeder(database).seedIfNeeded()
        val memberships = repository.observeMembershipsForUser(DemoSeeder.EMAIL_DEMO_USER).first()
        val bharatClerk = memberships.first { it.companyName == "Bharat Cargo Carriers" }
        val bcBranch = repository.getBranchesForCompany(bharatClerk.companyId).first()

        val result = repository.selectCompanyAndBranch(bharatClerk.localId, bcBranch.localId)

        assertTrue(result.isSuccess())
        val session = sessionStore.session.first()
        assertEquals("Bharat Cargo Carriers", session.companyName)
        assertEquals("Nagpur", session.branchName)
    }

    @Test
    fun `accepting an invitation activates the membership and enqueues an outbox update`() = runTest {
        DemoSeeder(database).seedIfNeeded()
        val invite = repository.observeMembershipsForUser(DemoSeeder.EMAIL_DEMO_USER).first()
            .first { it.status == MembershipStatus.INVITED }

        val result = repository.setInvitationAccepted(invite.localId)

        assertTrue(result.isSuccess())
        val updated = repository.observeMembershipsForUser(DemoSeeder.EMAIL_DEMO_USER).first()
            .first { it.localId == invite.localId }
        assertEquals(MembershipStatus.ACTIVE, updated.status)
        assertEquals(1, database.outboxDao().getPendingCount())
    }

    @Test
    fun `register company writes company branch membership and dependency-aware outbox rows`() = runTest {
        val result = repository.registerCompany(
            RegisterCompanyRequest(
                companyName = "Test Transport Co",
                address = "Test Address",
                gstin = null,
                branchName = "Indore",
                branchCode = "TST",
                ownerUserName = "Mahesh Patidar",
                ownerUserEmail = DemoSeeder.EMAIL_DEMO_USER,
            ),
        )

        assertTrue(result.isSuccess())
        val session = sessionStore.session.first()
        assertEquals("Test Transport Co", session.companyName)
        assertEquals(3, database.outboxDao().getPendingCount())

        // Dependency-aware readiness: only the company row is drainable until it completes.
        val ready = database.outboxDao().getReady(now = Long.MAX_VALUE)
        assertEquals(1, ready.size)
        assertEquals(com.example.transportapp.core.database.outbox.OutboxEntityType.COMPANY, ready.first().entity_type)
        assertTrue(ready.first().state == OutboxState.PENDING)
    }
}
