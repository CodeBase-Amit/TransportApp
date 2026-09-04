package com.example.transportapp.feature.auth

import com.example.transportapp.core.common.Result
import com.example.transportapp.data.transport.company.CompanyRepository
import com.example.transportapp.data.transport.numbering.NumberingRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.domain.transport.org.BranchSummary
import com.example.transportapp.domain.transport.org.CompanySummary
import com.example.transportapp.domain.transport.org.MembershipSummary
import com.example.transportapp.domain.transport.org.RegisterCompanyRequest
import com.example.transportapp.feature.auth.screen.SetupField
import com.example.transportapp.feature.auth.screen.SetupWizardEvent
import com.example.transportapp.feature.auth.screen.SetupWizardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * S18: the wizard's Finish actually registers what the user typed — both wires were dead
 * before (AgentChanges S18): Finish called the nav callback directly and no field wrote back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupWizardViewModelTest {

    private class FakeSessionRepository(initial: UserSession) : SessionRepository {
        private val state = MutableStateFlow(initial)
        override val session: Flow<UserSession> = state
        var signedIn = false
        var updateDisplayNameCalled = false
        override suspend fun updateDisplayName(name: String) {
            updateDisplayNameCalled = true
        }
        override suspend fun signIn() {
            updateDisplayNameCalled = true
            signedIn = true
        }
        override suspend fun signInWithPassword(email: String, password: String) = com.example.transportapp.core.common.Result.success(Unit)
        override suspend fun signOut() {
            state.value = state.value.copy(userId = "", name = "", email = "", role = "", companyId = "", companyName = "", branchId = "", branchName = "")
        }
        fun set(session: UserSession) {
            state.value = session
        }
    }

    private class FakeCompanyRepository : CompanyRepository {
        var registered: RegisterCompanyRequest? = null
        override fun observeCompanies(): Flow<List<CompanySummary>> = flowOf(emptyList())
        override fun observeAllBranches(): Flow<List<BranchSummary>> = flowOf(emptyList())
        override fun observeMemberCounts(): Flow<Map<String, Int>> = flowOf(emptyMap())
        override fun observeMembershipsForUser(userEmail: String): Flow<List<MembershipSummary>> = flowOf(emptyList())
        override suspend fun getBranchesForCompany(companyLocalId: String): List<BranchSummary> = emptyList()
        override suspend fun selectCompanyAndBranch(membershipLocalId: String, branchLocalId: String?): Result<Unit> = Result.success(Unit)
        override suspend fun setInvitationAccepted(membershipLocalId: String): Result<Unit> = Result.success(Unit)
        override suspend fun setInvitationDeclined(membershipLocalId: String): Result<Unit> = Result.success(Unit)
        override suspend fun registerCompany(request: RegisterCompanyRequest): Result<Unit> {
            registered = request
            return Result.success(Unit)
        }
    }

    private class FakeNumberingRepository : NumberingRepository {
        var ensured: Triple<String, String, String>? = null
        override suspend fun peekNext(companyId: String, branchId: String, docType: String) = null
        override suspend fun issueNext(companyId: String, branchId: String, docType: String, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.LEASE_EXHAUSTED)
        override suspend fun debugShrinkActiveLease(companyId: String, branchId: String, docType: String) {}
        override suspend fun debugSetGrantsEnabled(enabled: Boolean) {}
        override suspend fun ensureSeries(companyId: String, branchId: String, docType: String, prefix: String) {
            ensured = Triple(companyId, branchId, docType)
        }
    }

    private val signedIn = UserSession(
        userId = "u", name = "Ramesh Verma", email = "ramesh@example.in", role = "OWNER",
        companyId = "", companyName = "", branchId = "", branchName = "",
    )

    private val session = FakeSessionRepository(signedIn)
    private val companies = FakeCompanyRepository()
    private val numbering = FakeNumberingRepository()
    private lateinit var viewModel: SetupWizardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SetupWizardViewModel(companies, session, numbering)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun typeEverything() {
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.COMPANY_NAME, "  Verma Transport Co  "))
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.HEAD_OFFICE, "12 Station Road, Bhiwandi"))
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.GSTIN, "27aabcv1234k1z5"))
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.BRANCH_NAME, "Bhiwandi"))
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.BRANCH_CODE, "bwd"))
    }

    @Test
    fun `finish registers the typed values - not sample furniture`() = runTest {
        typeEverything()
        viewModel.onEvent(SetupWizardEvent.Finish)

        val request = companies.registered
        assertNotNull("registerCompany must run from Finish", request)
        assertEquals("Verma Transport Co", request?.companyName)
        assertEquals("12 Station Road, Bhiwandi", request?.address)
        assertEquals("27AABCV1234K1Z5", request?.gstin)
        assertEquals("Bhiwandi", request?.branchName)
        assertEquals("BWD", request?.branchCode)
        assertEquals("ramesh@example.in", request?.ownerUserEmail)
        assertTrue(viewModel.uiState.value.justFinished)
    }

    @Test
    fun `finish provisions the branch bilty series`() = runTest {
        typeEverything()
        viewModel.onEvent(SetupWizardEvent.Finish)

        assertEquals("BILTY", numbering.ensured?.third)
    }

    @Test
    fun `finish without a company name is refused - nothing registers`() = runTest {
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.BRANCH_CODE, "BWD"))
        viewModel.onEvent(SetupWizardEvent.Finish)

        assertTrue(companies.registered == null)
        assertFalse(viewModel.uiState.value.justFinished)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun `edit fields reformat - gstin and branch code uppercased`() = runTest {
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.COMPANY_NAME, "Verma"))
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.BRANCH_CODE, "bwd"))
        viewModel.onEvent(SetupWizardEvent.EditField(SetupField.GSTIN, "27aabcv1234k1z5"))

        assertEquals("bwd".uppercase(), viewModel.uiState.value.branchCode)
        assertEquals("27aabcv1234k1z5".uppercase(), viewModel.uiState.value.gstin)
        assertEquals("BWD/2627/00001", viewModel.uiState.value.nextBilty)
    }
}
