package com.example.transportapp.feature.booking.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BiltyPreviewUiState(
    val biltyNo: String = "IND/2627/04188",
    val copyCount: Int = 4,
    val grandTotalFormatted: String = "3,944.00"
)

class BiltyPreviewViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BiltyPreviewUiState())
    val uiState: StateFlow<BiltyPreviewUiState> = _uiState.asStateFlow()
}