package com.example.transportapp.feature.auth.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CarouselViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(CarouselUiState())
    val uiState: StateFlow<CarouselUiState> = _uiState.asStateFlow()

    fun onEvent(event: CarouselEvent) {
        when (event) {
            CarouselEvent.Next -> _uiState.update {
                it.copy(currentPage = (it.currentPage + 1).coerceAtMost(it.panels.lastIndex))
            }
            is CarouselEvent.SelectPage -> _uiState.update {
                it.copy(currentPage = event.page.coerceIn(0, it.panels.lastIndex))
            }
        }
    }
}
