package com.example.transportapp.feature.masters.screen

import com.example.transportapp.core.ui.sample.MasterGroup
import com.example.transportapp.core.ui.sample.MastersHubSampleData

data class MastersHubUiState(
    val title: String = MastersHubSampleData.TITLE,
    val subtitle: String = MastersHubSampleData.SUBTITLE,
    val groups: List<MasterGroup> = MastersHubSampleData.groups,
    val duplicateBanner: String = MastersHubSampleData.DUPLICATE_BANNER,
    val duplicateAction: String = MastersHubSampleData.DUPLICATE_ACTION
)

sealed interface MastersHubEvent {
    data object ReviewDuplicates : MastersHubEvent
}
