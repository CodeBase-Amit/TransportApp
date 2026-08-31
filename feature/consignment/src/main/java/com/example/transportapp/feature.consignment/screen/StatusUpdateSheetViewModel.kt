package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.tracking.NewStatusEvent
import com.example.transportapp.data.transport.tracking.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T9's real save (Phase2.md S8): the sheet offers only the §7.1-legal continuations, the
 * hold path carries its reason and ≥10-character remark, and the save appends the event
 * with the projection advancing with it (D1).
 */
@HiltViewModel
class StatusUpdateSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val statusRepository: StatusRepository,
) : ViewModel() {

    private val biltyNo: String = checkNotNull(savedStateHandle["biltyNo"])

    private val _uiState = MutableStateFlow(StatusUpdateSheetUiState(biltyNo = biltyNo))
    val uiState: StateFlow<StatusUpdateSheetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            val current = statusRepository.currentStatus(biltyNo)
            val options = statusRepository.legalNext(biltyNo).map { target ->
                SheetOption(
                    target = target,
                    label = labelOf(target, session.branchName),
                    detail = "Legal next step from the current status",
                    holdPath = target == com.example.transportapp.domain.transport.ConsignmentStatus.HELD,
                )
            }
            _uiState.update {
                it.copy(
                    contextLine = current?.let { c -> "currently ${c.wording.lowercase()}" } ?: "",
                    options = options,
                    selected = options.firstOrNull(),
                )
            }
        }
    }

    private fun labelOf(target: com.example.transportapp.domain.transport.ConsignmentStatus, branchName: String): String = when (target) {
        com.example.transportapp.domain.transport.ConsignmentStatus.LOADED -> "Loaded — on the challan"
        com.example.transportapp.domain.transport.ConsignmentStatus.IN_TRANSIT -> "Departed — back in transit"
        com.example.transportapp.domain.transport.ConsignmentStatus.AT_HUB -> "At hub"
        com.example.transportapp.domain.transport.ConsignmentStatus.ARRIVED -> "Arrived at $branchName"
        com.example.transportapp.domain.transport.ConsignmentStatus.OUT_FOR_DELIVERY -> "Out for delivery"
        com.example.transportapp.domain.transport.ConsignmentStatus.DELIVERED -> "Delivered — POD captured"
        com.example.transportapp.domain.transport.ConsignmentStatus.HELD -> "Hold — exception"
        com.example.transportapp.domain.transport.ConsignmentStatus.RETURNED -> "Return to origin — RTO"
        else -> target.wording
    }

    fun onEvent(event: StatusUpdateSheetEvent) {
        when (event) {
            is StatusUpdateSheetEvent.SelectOption -> _uiState.update { it.copy(selected = event.option) }
            is StatusUpdateSheetEvent.SelectHoldReason -> _uiState.update { it.copy(holdReason = event.reason) }
            is StatusUpdateSheetEvent.ChangeRemark -> _uiState.update { it.copy(remark = event.value) }
            is StatusUpdateSheetEvent.ChangeLocation -> _uiState.update { it.copy(location = event.value) }
            StatusUpdateSheetEvent.UseMyLocation -> _uiState.update { it.copy(location = "Current town") }
            StatusUpdateSheetEvent.Save -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        val selected = state.selected ?: return
        if (state.isSaving) return
        if (selected.holdPath && state.remark.length < 10) {
            _uiState.update { it.copy(error = "A hold needs a remark of at least ten characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val result = statusRepository.append(
                NewStatusEvent(
                    biltyNo = biltyNo,
                    eventType = selected.target.name,
                    location = state.location.takeIf { it.isNotBlank() },
                    remark = state.remark.takeIf { it.isNotBlank() },
                    reasonCode = if (selected.holdPath) state.holdReason.code else null,
                ),
                System.currentTimeMillis(),
            )
            val failure = result as? com.example.transportapp.core.common.Result.Failure
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = failure?.message ?: failure?.code?.name,
                    saved = result.isSuccess(),
                )
            }
        }
    }
}
