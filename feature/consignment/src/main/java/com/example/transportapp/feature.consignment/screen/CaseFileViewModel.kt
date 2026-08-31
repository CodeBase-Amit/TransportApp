package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.designsystem.component.StepState
import com.example.transportapp.core.ui.sample.CaseEvent
import com.example.transportapp.core.ui.sample.CaseFileMoneyLine
import com.example.transportapp.core.ui.sample.CaseFileStat
import com.example.transportapp.data.transport.consignment.CaseFileRepository
import com.example.transportapp.data.transport.session.SessionRepository
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
) : ViewModel() {

    private val biltyNo: String = checkNotNull(savedStateHandle["biltyNo"])

    private val _uiState = MutableStateFlow(CaseFileUiState(biltyNo = biltyNo))
    val uiState: StateFlow<CaseFileUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Re-reads on resume so a T9 save shows up the moment the user returns (Design T8's V5 moment). */
    fun refresh() {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
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
