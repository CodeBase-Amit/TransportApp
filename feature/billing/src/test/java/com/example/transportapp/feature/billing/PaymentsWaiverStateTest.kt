package com.example.transportapp.feature.billing

import com.example.transportapp.core.common.Result
import com.example.transportapp.data.transport.billing.AllocationInput
import com.example.transportapp.data.transport.billing.BillConsignmentLine
import com.example.transportapp.data.transport.billing.BillDetail
import com.example.transportapp.data.transport.billing.BillHeader
import com.example.transportapp.data.transport.billing.BillingRepository
import com.example.transportapp.data.transport.billing.OutstandingBill
import com.example.transportapp.data.transport.billing.PartyChoice
import com.example.transportapp.data.transport.billing.PoolFilter
import com.example.transportapp.data.transport.billing.ReceiptLine
import com.example.transportapp.data.transport.billing.ReceiptSaved
import com.example.transportapp.data.transport.billing.Statement
import com.example.transportapp.data.transport.billing.TopayLine
import com.example.transportapp.data.transport.billing.UnbilledPartyGroup
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.session.UserSession
import com.example.transportapp.data.transport.tracking.StatusRepository
import com.example.transportapp.feature.billing.screen.PaymentsEvent
import com.example.transportapp.feature.billing.screen.PaymentsViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * S27 regression (T15 waiver path): recordWaiver never set `waiving` while the call was in
 * flight â€” the sheet's "Recordingâ€¦" state could never appear and the button allowed
 * double-taps. The sheet must flip to in-flight before the repo call and land on a waived
 * line after it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentsWaiverStateTest {

    private class FakeBilling : BillingRepository {
        override fun observeUnbilledPool(filter: PoolFilter) = flowOf(emptyList<UnbilledPartyGroup>())
        override fun observeUnbilledRows(partyId: String, filter: PoolFilter) = flowOf(emptyList<BillConsignmentLine>())
        override suspend fun buildDraftBill(partyId: String, consignmentIds: List<String>, dueAt: Long?, notes: String?, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun removeConsignmentFromDraft(billId: String, consignmentId: String, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun cancelBill(billId: String, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun issueBill(billId: String, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override fun observeBill(billId: String) = flowOf<BillDetail?>(null)
        override fun observeTopayAwaiting() = flowOf(emptyList<TopayLine>())
        override fun observeRecentReceipts() = flowOf(emptyList<ReceiptLine>())
        override suspend fun receiptsSinceSummary(sinceAt: Long) = 0L to 0
        override suspend fun recordReceipt(payerPartyId: String, amountPaise: Long, instrument: String, instrumentRef: String?, allocations: List<AllocationInput>, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun outstandingBillsForParty(partyId: String) = emptyList<OutstandingBill>()
        override suspend fun partiesWithIssuedBills() = emptyList<PartyChoice>()
        override suspend fun statement(partyId: String, periodStart: Long, periodEnd: Long, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
    }

    private class FakeStatus : StatusRepository {
        lateinit var gate: CompletableDeferred<Result<Unit>>
        override suspend fun append(event: com.example.transportapp.data.transport.tracking.NewStatusEvent, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun rebuildProjection(consignmentId: String) = null
        override suspend fun bulkAppendByChallan(challanNo: String, eventType: String, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun legalNext(biltyNo: String) = emptyList<com.example.transportapp.domain.transport.ConsignmentStatus>()
        override suspend fun currentStatus(biltyNo: String) = null
        override suspend fun recordPod(biltyNo: String, consigneeName: String, signatureRef: String?, photoRef: String?, remarks: String?, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun addAttachment(biltyNo: String, kind: String, source: android.net.Uri, caption: String?, now: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun waiveTopPay(biltyNo: String, reason: String, now: Long): Result<Unit> = gate.await()
        override suspend fun exceptions(companyId: String, branchId: String?, sinceAt: Long, now: Long) = emptyList<com.example.transportapp.data.transport.tracking.ExceptionItem>()
        override suspend fun countOverdue(companyId: String, branchId: String?, now: Long) = 0
    }

    private class FakeSession : SessionRepository {
        override val session: Flow<UserSession> = MutableStateFlow(
            UserSession("u", "O", "o@x.in", "MANAGER", "c1", "Co", "b1", "Indore"),
        )
        override suspend fun signIn() {}
        override suspend fun signInWithPassword(email: String, password: String) = Result.success(Unit)
        override suspend fun updateDisplayName(name: String) {}
        override suspend fun signOut() {}
    }

    private class FakeDocuments : com.example.transportapp.data.transport.documents.DocumentRepository {
        override suspend fun copyLabels(biltyNo: String) = emptyList<String>()
        override suspend fun biltyValues(biltyNo: String) = null
        override suspend fun renderBilty(biltyNo: String, copies: List<String>) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun renderChallan(challanNo: String) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun renderFreightBill(billId: String) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun renderReceipt(receiptId: String) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override suspend fun renderStatement(partyId: String, from: Long, to: Long) = Result.failure(com.example.transportapp.core.common.ErrorCode.MASTER_IN_USE)
        override fun share(document: com.example.transportapp.data.transport.documents.RenderedDocument, chooserTitle: String) {}
        override fun print(document: com.example.transportapp.data.transport.documents.RenderedDocument) {}
        override fun saveToDownloads(document: com.example.transportapp.data.transport.documents.RenderedDocument): android.net.Uri? = null
    }

    private val status = FakeStatus()

    private val line = TopayLine(
        localId = "l1", displayNo = "B/1", consigneePartyId = "p1", consigneeName = "Party",
        amountPaise = 100_00, status = "HELD", heldRemark = "shortage", waived = false,
    )

    @Before
    fun setUp() {
        Dispatchers.resetMain()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `waiver flips the sheet in-flight then lands waived`() = runTest {
        val mainDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        val vm = PaymentsViewModel(FakeBilling(), status, FakeSession(), FakeDocuments())

        vm.onEvent(PaymentsEvent.OpenCollect(line))
        status.gate = CompletableDeferred()

        vm.onEvent(PaymentsEvent.RecordWaiver)

        assertTrue("the sheet must be in-flight while the waiver records", vm.uiState.value.collectSheet!!.waiving)

        status.gate.complete(Result.success(Unit))
        advanceUntilIdle()

        val sheet = vm.uiState.value.collectSheet!!
        assertFalse("in-flight must clear when the waiver lands", sheet.waiving)
        assertTrue("the line is collectable once waived", sheet.line.waived)
    }
}
