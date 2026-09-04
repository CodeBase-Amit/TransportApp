package com.example.transportapp.data.transport.templates

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.common.SeedIds
import com.example.transportapp.core.database.TransportDatabase
import com.example.transportapp.core.database.seed.DemoSeeder
import com.example.transportapp.data.transport.outbox.OutboxWriter
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S11: templates as data — the seeded default BILTY template resolves active at version 1
 * and parses clean through the engine; a malformed install is refused with a typed code and
 * writes nothing; a valid install becomes version 2 and atomically flips active.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TemplateRepositoryTest {

    private lateinit var database: TransportDatabase
    private lateinit var repository: TemplateRepositoryImpl
    private val company = SeedIds.COMPANY_SHIVSHAKTI
    private val now = System.currentTimeMillis()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DemoSeeder(database).seedIfNeeded()
        repository = TemplateRepositoryImpl(
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
            OutboxWriter(database.outboxDao()),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `the seeded bilty template resolves active at version 1 and parses clean`() = runTest {
        val active = repository.getActiveTemplate("tpl-bilty-default")
        assertNotNull(active)
        assertEquals(1, active!!.version)
        assertEquals(1, active.schemaVersion)
        assertEquals("Default Bilty", active.model.name)
        assertEquals("SHIVSHAKTI ROADLINES", active.model.business.shopName)
        assertEquals(7, active.model.sections.size)

        val list = repository.observeTemplates().first()
        assertEquals(1, list.size)
        assertTrue(list.single().isActive)
        assertEquals("BUILT-IN", list.single().visibility)
    }

    @Test
    fun `a malformed install is refused with a typed code and writes nothing`() = runTest {
        val before = database.templateDao().countTemplates(company)

        val refused = repository.installTemplate("tpl-bilty-default", "{ not json ", now)
        assertTrue(refused is Result.Failure)

        val future = repository.installTemplate(
            "tpl-bilty-default",
            goodTemplateJson().replace("\"schemaVersion\": 1", "\"schemaVersion\": 99"),
            now,
        )
        assertEquals(ErrorCode.TEMPLATE_VERSION_MISSING, (future as Result.Failure).code)

        assertEquals("nothing written", before, database.templateDao().countTemplates(company))
        assertEquals("the seed version is still the active one", 1, repository.getActiveTemplate("tpl-bilty-default")!!.version)
    }

    @Test
    fun `a valid install becomes version 2 and atomically flips active`() = runTest {
        val result = repository.installTemplate("tpl-bilty-default", goodTemplateJson(), now).getOrNull()!!
        assertEquals(2, result.version)
        assertTrue(result.isActive)

        val active = repository.getActiveTemplate("tpl-bilty-default")!!
        assertEquals("the new row is what resolves active", 2, active.version)
        assertEquals("Renamed Bilty", active.model.name)
        assertEquals("exactly one active row", 1, database.templateDao().observeTemplates(company).first().count { it.is_active })
        assertEquals(2, database.templateDao().observeTemplates(company).first().size)
        assertTrue("the new template went to the outbox", database.outboxDao().getPendingCount() > 0)

        // The pinned-version lookup keeps version 1 resolvable for reprints (§9.12).
        val pinned = repository.getTemplateVersion("tpl-bilty-default", 1)
        assertNotNull(pinned)
        assertEquals("Default Bilty", pinned!!.model.name)
    }

    private fun goodTemplateJson(): String = """
        {
          "schemaVersion": 1,
          "id": "tpl-bilty-default",
          "name": "Renamed Bilty",
          "version": 2,
          "business": { "shopName": "SHIVSHAKTI ROADLINES" },
          "sections": [ { "type": "header" } ]
        }
    """.trimIndent()
}
