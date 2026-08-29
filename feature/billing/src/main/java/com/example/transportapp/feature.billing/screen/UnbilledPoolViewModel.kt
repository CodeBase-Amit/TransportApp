package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UnbilledPoolViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UnbilledPoolUiState())
    val uiState: StateFlow<UnbilledPoolUiState> = _uiState.asStateFlow()

    fun onEvent(event: UnbilledPoolEvent) {
        when (event) {
            is UnbilledPoolEvent.SelectAll -> _uiState.update {
                val allSelected = it.parties.all { p -> p.selected }
                it.copy(parties = it.parties.map { p -> p.copy(selected = !allSelected) })
            }
            is UnbilledPoolEvent.ToggleSelect -> _uiState.update {
                it.copy(parties = it.parties.map { p ->
                    if (p.name == event.name) p.copy(selected = !p.selected) else p
                })
            }
            is UnbilledPoolEvent.ToggleExpand -> _uiState.update {
                it.copy(parties = it.parties.map { p ->
                    if (p.name == event.name) p.copy(expanded = !p.expanded) else p
                })
            }
            is UnbilledPoolEvent.SelectFilter -> _uiState.update { it.copy(selectedFilter = event.filter) }
        }
    }
}
