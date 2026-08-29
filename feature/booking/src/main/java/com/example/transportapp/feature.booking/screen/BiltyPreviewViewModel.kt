package com.example.transportapp.feature.booking.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BiltyPreviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BiltyPreviewUiState())
    val uiState: StateFlow<BiltyPreviewUiState> = _uiState.asStateFlow()
}
