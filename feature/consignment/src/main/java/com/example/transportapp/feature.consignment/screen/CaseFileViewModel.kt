package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.ui.PrintStatus
import com.example.transportapp.core.ui.sample.CaseEvent
import com.example.transportapp.core.ui.sample.CaseFileMoneyLine
import com.example.transportapp.core.ui.sample.CaseFileStat
import com.example.transportapp.data.transport.consignment.CaseFileRepository
import com.example.transportapp.data.transport.consignment.ConsignmentRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.tracking.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaseDocumentUi(
    val title: String,
    val detail: String,
    val trailing: String?,
    val action: Boolean = false,
)

data class CaseFileUiState(
    val biltyNo: String = "",
    val stats: List<CaseFileStat> = emptyList(),
    val events: List<CaseEvent> = emptyList(),
    val documents: List<CaseDocumentUi> = emptyList(),
    val moneyRows: List<CaseFileMoneyLine> = emptyList(),
    val recordLines: List<String> = emptyList(),
    val fromStation: String = "",
    val toStation: String = "",
    val distance: String = "",
    val bookedText: String = "",
    val toPayCallout: String? = null,
    val paymentMode: com.example.transportapp.domain.transport.PaymentMode = com.example.transportapp.domain.transport.PaymentMode.TOPAY,
    val status: com.example.transportapp.domain.transport.ConsignmentStatus = com.example.transportapp.domain.transport.ConsignmentStatus.BOOKED,
    val isLoading: Boolean = true,
)

/**
 * T8 (Phase2.md S6): one consignment's whole story from local storage — header, live
 * timeline from the append-only event log, documents, money position, record lines.
 */
@HiltViewModel
class CaseFileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val caseFileRepository: CaseFileRepository,
    private val consignmentRepository: ConsignmentRepository,
    private val statusRepository: StatusRepository,
    private val documentRepository: com.example.transportapp.data.transport.documents.DocumentRepository,
) : ViewModel() {

    private val biltyNo: String = checkNotNull(savedStateHandle["biltyNo"])

    private val _uiState = MutableStateFlow(CaseFileUiState(biltyNo = biltyNo))
    val uiState: StateFlow<CaseFileUiState> = _uiState.asStateFlow()

    /** The reprint status line (S13): rendering takes a beat; errors are dismissible copy. */
    private val _printStatus = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
    val printStatus: StateFlow<PrintStatus> = _printStatus.asStateFlow()

    init {
        refresh()
    }

    /**
     * Reprint from the stored snapshot (§9.12): the pinned template version renders the
     * document exactly as first printed, through the byte path once rendered.
     */
    fun printBilty() {
        if (_printStatus.value is PrintStatus.Rendering) return
        _printStatus.value = PrintStatus.Rendering("Reprinting from the stored snapshot…")
        viewModelScope.launch {
            val labels = documentRepository.copyLabels(biltyNo)
            when (val result = documentRepository.renderBilty(biltyNo, labels)) {
                is com.example.transportapp.core.common.Result.Success -> {
                    _printStatus.value = PrintStatus.Idle
                    documentRepository.print(result.value)
                }
                is com.example.transportapp.core.common.Result.Failure ->
                    _printStatus.value = PrintStatus.Error(result.message ?: "The document could not be printed")
            }
        }
    }

    fun dismissPrintStatus() {
        _printStatus.value = PrintStatus.Idle
    }

    /** §7.1 Manager-gated cancel with a §7.2-strength reason; the number is retained. */
    fun cancelBilty(reason: String) {
        if (_printStatus.value is PrintStatus.Rendering) return
        _printStatus.value = PrintStatus.Rendering("Cancelling…")
        viewModelScope.launch {
            when (val result = consignmentRepository.cancel(biltyNo, reason)) {
                is com.example.transportapp.core.common.Result.Success -> {
                    _printStatus.value = PrintStatus.Idle
                    refresh()
                }
                is com.example.transportapp.core.common.Result.Failure ->
                    _printStatus.value = PrintStatus.Error(result.message ?: "The bilty could not be cancelled")
            }
        }
    }

    /** Whether this member may amend/cancel (§17.4.1: Manager and above). */
    private val _canManage = MutableStateFlow(false)
    val canManage: StateFlow<Boolean> = _canManage.asStateFlow()

    /** S15: the add-photo path writes a local file and enqueues the ATTACHMENT_E row. */
    fun addPhoto() {
        if (_printStatus.value is PrintStatus.Rendering) return
        viewModelScope.launch {
            val result = statusRepository.addAttachment(
                biltyNo = biltyNo,
                kind = "GOODS",
                fileRef = "attachments/att-${System.currentTimeMillis()}.jpg",
                sizeBytes = 0,
                caption = "Captured at the case file",
                now = System.currentTimeMillis(),
            )
            when (result) {
                is com.example.transportapp.core.common.Result.Success -> refresh()
                is com.example.transportapp.core.common.Result.Failure ->
                    _printStatus.value = PrintStatus.Error(result.message ?: "The photo could not be attached")
            }
        }
    }

    /** Re-reads on resume so a T9 save shows up the moment the user returns (Design T8's V5 moment). */
    fun refresh() {        viewModelScope.launch {
            val session = sessionRepository.session.first()
            _canManage.value = session.role == "OWNER" || session.role == "MANAGER"
            val case = caseFileRepository.caseFile(session.companyId, biltyNo, session.branchName, System.currentTimeMillis())
            _uiState.update { state ->
                when (case) {
                    null -> state.copy(isLoading = false)
                    else -> state.copy(
                        biltyNo = case.biltyNo,
                        stats = listOf(
                            CaseFileStat("Packages", case.packages.toString()),
                            CaseFileStat("Chargeable", "${case.chargeableKg} kg"),
                            CaseFileStat("Freight", Money(case.money.first { it.strong }.amountPaise).formatted()),
                            CaseFileStat("Expected", case.expectedText),
                        ),
                        events = case.events.mapIndexed { index, event ->
                            // Newest real event is the current tick; earlier ones are done;
                            // an unreached tick draws hollow (Design T8's vertical line).
                            val step = when {
                                event.unreached -> StepState.UPCOMING
                                index == case.events.indexOfLast { !it.unreached } -> StepState.CURRENT
                                else -> StepState.DONE
                            }
                            CaseEvent(
                                name = event.type,
                                station = event.location ?: "",
                                time = event.atText ?: "",
                                actor = event.actor,
                                state = step,
                            )
                        },
                        documents = case.documents.map { doc ->
                            CaseDocumentUi(
                                title = doc.title,
                                detail = doc.number ?: doc.trailing ?: "",
                                trailing = if (doc.number != null) doc.trailing else null,
                                action = doc.actionable && doc.number == null,
                            )
                        },
                        moneyRows = case.money.map { CaseFileMoneyLine(it.label, Money(it.amountPaise).formatted(), it.strong) },
                        recordLines = case.recordLines,
                        fromStation = case.fromStation,
                        toStation = case.toStation,
                        distance = case.distanceKm?.let { "$it km" } ?: "",
                        bookedText = case.bookedText,
                        toPayCallout = case.toPayCallout,
                        paymentMode = case.paymentMode ?: com.example.transportapp.domain.transport.PaymentMode.TOPAY,
                        status = case.status,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
