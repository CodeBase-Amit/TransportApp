package com.example.transportapp.feature.masters

import com.example.transportapp.core.common.ErrorCode
import com.example.transportapp.core.common.Result
import com.example.transportapp.domain.transport.masters.AutoCharge
import com.example.transportapp.domain.transport.masters.DuplicatePair
import com.example.transportapp.domain.transport.masters.MasterCounts
import com.example.transportapp.domain.transport.masters.PartyDetail
import com.example.transportapp.domain.transport.masters.PartyListRow
import com.example.transportapp.domain.transport.masters.RateRow
import com.example.transportapp.data.transport.masters.MastersRepository
import com.example.transportapp.feature.masters.screen.MasterEditorEvent
import com.example.transportapp.feature.masters.screen.MasterEditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** S3 ViewModel test (Phase2.md Â§7): editor validation against a fake repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class MasterEditorViewModelTest {

    

    private class FakeMastersRepository : MastersRepository {
        var saved: Pair<String?, String>? = null
        var deleteResult: Result<Unit> = Result.success(Unit)

        override suspend fun counts(companyId: String) = MasterCounts(0, 0, 0, 0, 0, 0, 0, 0, 0)
        override fun observeParties(companyId: String, query: String, letter: String, duplicatesOnly: Boolean): Flow<List<PartyListRow>> = flowOf(emptyList())
        override fun observeDuplicateCount(companyId: String): Flow<Int> = flowOf(0)
        override fun observeDuplicatePair(companyId: String): Flow<DuplicatePair?> = flowOf(null)
        override suspend fun resolveParty(idOrName: String): PartyDetail? = null
        override suspend fun partyDetail(localId: String): PartyDetail? = null
        override suspend fun createOrUpdateParty(
            companyId: String, localId: String?, name: String, phone: String, email: String?,
            street: String?, station: String?, pincode: String?, gstin: String?,
            type: String, usualRouteId: String?, usualPaymentMode: String?,
        ): Result<String> {
            saved = localId to name
            return if (name.isBlank()) Result.failure(ErrorCode.MASTER_IN_USE, "Party name is required")
            else Result.success("new-id-1")
        }
        override suspend fun deleteParty(localId: String): Result<Unit> = deleteResult
        override suspend fun mergeParties(keepId: String, mergeId: String): Result<Unit> = Result.success(Unit)
        override suspend fun rateRowsForParty(partyId: String): List<RateRow> = emptyList()
        override suspend fun autoCharges(companyId: String): List<AutoCharge> = emptyList()
        override suspend fun saveRateRow(localId: String, ratePaise: Long): Result<Unit> = Result.success(Unit)
        override suspend fun searchPartiesOnce(companyId: String, query: String) = emptyList<com.example.transportapp.domain.transport.masters.PartyListRow>()
    }

    private val sessionFlow = MutableStateFlow(
        com.example.transportapp.data.transport.session.UserSession(
            userId = "u", name = "Mahesh Patidar", email = "mahesh.patidar@gmail.com", role = "OWNER",
            companyId = "c1", companyName = "Shivshakti Roadlines", branchId = "b1", branchName = "Indore",
        ),
    )

    private val sessionRepository = object : com.example.transportapp.data.transport.session.SessionRepository {
        override val session: kotlinx.coroutines.flow.Flow<com.example.transportapp.data.transport.session.UserSession> = sessionFlow
        override suspend fun signOut() {}
    }

    private lateinit var repo: FakeMastersRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeMastersRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = MasterEditorViewModel(
        savedStateHandle = androidx.lifecycle.SavedStateHandle(mapOf("type" to "party", "id" to "new")),
        mastersRepository = repo,
        sessionRepository = sessionRepository,
    )

    @Test
    fun `blank name save surfaces the validation error`() = runTest {
        val vm = viewModel()
        vm.onEvent(MasterEditorEvent.ChangeName(""))
        vm.onEvent(MasterEditorEvent.Save)

        val state = vm.uiState.value
        assertEquals("Party name is required", state.error)
        assertTrue(state.justSaved.not())
    }

    @Test
    fun `valid save persists and flips justSaved`() = runTest {
        val vm = viewModel()
        vm.onEvent(MasterEditorEvent.ChangeName("Sharma Traders"))
        vm.onEvent(MasterEditorEvent.ChangePhone("+91 99999 12345"))
        vm.onEvent(MasterEditorEvent.Save)

        assertEquals("Sharma Traders", repo.saved?.second)
        assertTrue(vm.uiState.value.justSaved)
        assertTrue(vm.uiState.value.error == null)
    }
}
