package com.example.transportapp.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.transportapp.core.database.seed.DemoSeeder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** S2: the org dataset (§B6) seeds once and matches the T2 ground truth. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DemoSeederTest {

    private lateinit var database: TransportDatabase
    private lateinit var seeder: DemoSeeder

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, TransportDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seeder = DemoSeeder(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seeds the org dataset once`() = runTest {
        seeder.seedIfNeeded()
        seeder.seedIfNeeded() // second call must be a no-op

        val orgDao = database.orgDao()
        val companies = orgDao.observeCompanies().first()
        assertEquals(3, companies.size) // Shivshakti + Bharat Cargo + Malwa (invitation source)

        val memberships = orgDao.observeMemberships().first()
        assertEquals(6, memberships.size) // 4 Shivshakti members + 1 Bharat clerk + 1 invitation
        val owner = memberships.first { it.role == "OWNER" }
        assertEquals(DemoSeeder.EMAIL_DEMO_USER, owner.user_email)
        assertEquals(4, memberships.count { it.company_id == DemoSeeder.ID_SHIVSHAKTI && it.status == "ACTIVE" })
        assertNotNull(database.seedVersionDao().get())
    }
}
