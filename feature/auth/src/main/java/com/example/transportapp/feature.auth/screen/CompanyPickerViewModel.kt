package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CompanyPickerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CompanyPickerUiState())
    val uiState: StateFlow<CompanyPickerUiState> = _uiState.asStateFlow()

    fun onEvent(event: CompanyPickerEvent) {
        when (event) {
            is CompanyPickerEvent.SelectCompany -> _uiState.update { it ->
                val branch = it.companies.getOrNull(event.index)?.branches?.firstOrNull() ?: "Indore"
                it.copy(selectedIndex = event.index, selectedBranch = branch)
            }
            is CompanyPickerEvent.SelectBranch -> _uiState.update { it.copy(selectedBranch = event.branch) }
            is CompanyPickerEvent.OpenCompany -> Unit
            is CompanyPickerEvent.AcceptInvitation -> Unit
            is CompanyPickerEvent.DeclineInvitation -> Unit
            CompanyPickerEvent.RegisterCompany, CompanyPickerEvent.SignOut -> Unit
        }
    }
}
