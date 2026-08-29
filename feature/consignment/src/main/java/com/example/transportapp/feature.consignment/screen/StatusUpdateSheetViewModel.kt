package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.ViewModel
import com.example.transportapp.core.ui.sample.StatusUpdateSheetSampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class StatusUpdateSheetViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(StatusUpdateSheetUiState())
    val uiState: StateFlow<StatusUpdateSheetUiState> = _uiState.asStateFlow()

    fun onEvent(event: StatusUpdateSheetEvent) {
        when (event) {
            is StatusUpdateSheetEvent.SelectEvent -> _uiState.update { it.copy(selectedEvent = event.event) }
            is StatusUpdateSheetEvent.SelectHoldReason -> _uiState.update { it.copy(holdReason = event.reason) }
            is StatusUpdateSheetEvent.ChangeRemark -> _uiState.update { it.copy(remark = event.value) }
            is StatusUpdateSheetEvent.ChangeLocation -> _uiState.update { it.copy(location = event.value) }
            StatusUpdateSheetEvent.UseMyLocation -> _uiState.update { it.copy(location = StatusUpdateSheetSampleData.LOCATION) }
        }
    }
}
