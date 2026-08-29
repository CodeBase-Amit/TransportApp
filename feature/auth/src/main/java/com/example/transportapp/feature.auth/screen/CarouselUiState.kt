package com.example.transportapp.feature.auth.screen

import com.example.transportapp.core.ui.sample.CarouselPanel
import com.example.transportapp.core.ui.sample.CarouselSampleData

data class CarouselUiState(
    val currentPage: Int = 0,
    val panels: List<CarouselPanel> = CarouselSampleData.PANELS,
    val skipLabel: String = CarouselSampleData.SKIP,
    val nextLabel: String = CarouselSampleData.NEXT,
    val getStartedLabel: String = CarouselSampleData.GET_STARTED,
    val usedBeforeLabel: String = CarouselSampleData.USED_BEFORE
)

sealed interface CarouselEvent {
    data object Next : CarouselEvent
    data class SelectPage(val page: Int) : CarouselEvent
    data object GetStarted : CarouselEvent
    data object Skip : CarouselEvent
}
