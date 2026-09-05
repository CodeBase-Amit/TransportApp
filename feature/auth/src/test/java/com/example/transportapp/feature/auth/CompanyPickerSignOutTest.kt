package com.example.transportapp.feature.auth

import com.example.transportapp.core.common.Result
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.feature.auth.screen.CompanyPickerEvent
import com.example.transportapp.feature.auth.screen.CompanyPickerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * S27 regression: the picker's sign-out icon navigated to Splash without going through the
 * ViewModel, so sessionRepository.signOut() never ran — Splash re-resolved straight back to
 * the picker and the tap did nothing. The event must sign out AND surface signedOut so the
 * screen navigates only after the session is cleared.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompanyPickerSignOutTest {

    private class FakeSessionRepository(initial: UserSession) : SessionRepository {
        private val state = MutableStateFlow(initial)
        override val session: Flow<UserSession> = state
        var signOutCalled = false
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signIn() {}
        override suspend fun signInWithPassword(email: String, password: String) = Result.success(Unit)
        override suspend fun signOut() {
            signOutCalled = true
            state.value = state.value.copy(userId = "", name = "", email = "", role = "", companyId = "", companyName = "", branchId = "", branchName = "")
        }
    }

    private class FakeCompanyRepository : com.example.transportapp.data.transport.company.CompanyRepository {
        override fun observeCompanies() = flowOf(emptyList<com.example.transportapp.domain.transport.org.CompanySummary>())
        override fun observeAllBranches() = flowOf(emptyList<com.example.transportapp.domain.transport.org.BranchSummary>())
        override fun observeMemberCounts() = flowOf(emptyMap<String, Int>())
        override fun observeMembershipsForUser(userEmail: String) = flowOf(emptyList<com.example.transportapp.domain.transport.org.MembershipSummary>())
        override suspend fun getBranchesForCompany(companyLocalId: String) = emptyList<com.example.transportapp.domain.transport.org.BranchSummary>()
        override suspend fun selectCompanyAndBranch(membershipLocalId: String, branchLocalId: String?) = Result.success(Unit)
        override suspend fun setInvitationAccepted(membershipLocalId: String) = Result.success(Unit)
        override suspend fun setInvitationDeclined(membershipLocalId: String) = Result.success(Unit)
        override suspend fun registerCompany(request: com.example.transportapp.domain.transport.org.RegisterCompanyRequest) = Result.success(Unit)
    }

    private val dispatcher = UnconfinedTestDispatcher()
    private val session = FakeSessionRepository(
        UserSession(
            userId = "u1", name = "Owner", email = "o@x.in", role = "OWNER",
            companyId = "c1", companyName = "Co", branchId = "b1", branchName = "Indore",
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sign out clears the session and flips signedOut`() = runTest {
        val vm = CompanyPickerViewModel(FakeCompanyRepository(), session)

        vm.onEvent(CompanyPickerEvent.SignOut)

        assertTrue("signOut() must run on the SignOut event", session.signOutCalled)
        assertTrue("the screen needs signedOut=true to navigate away after the session is cleared", vm.uiState.value.signedOut)
        assertTrue("the cleared session must be observable", session.session.first().companyId.isBlank())
    }
}
