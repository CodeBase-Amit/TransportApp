package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.masters.MastersRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.masters.MasterCounts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T17 — Masters hub (Phase2.md S3). The nine counts are live queries over Room; the
 * duplicate banner counts parties that share a phone number.
 */
@HiltViewModel
class MastersHubViewModel @Inject constructor(
    private val mastersRepository: MastersRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MastersHubUiState())
    val uiState: StateFlow<MastersHubUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val companyId = sessionRepository.session.first().companyId
            val counts: MasterCounts = mastersRepository.counts(companyId)
            val duplicates = mastersRepository.observeDuplicateCount(companyId).first()
            _uiState.update { MastersHubUiState.from(counts, duplicates) }
        }
    }
}
