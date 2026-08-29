package com.example.transportapp.feature.templates.screen

import com.example.transportapp.core.ui.sample.TemplateRow
import com.example.transportapp.core.ui.sample.TemplatesSampleData
import com.example.transportapp.core.ui.sample.VersionHistory

data class TemplatesUiState(
    val title: String = TemplatesSampleData.TITLE,
    val subtitle: String = TemplatesSampleData.SUBTITLE,
    val requestTemplate: String = TemplatesSampleData.REQUEST_TEMPLATE,
    val versionHistoryHeading: String = TemplatesSampleData.VERSION_HISTORY_HEADING,
    val templates: List<TemplateRow> = TemplatesSampleData.templates,
    val versionHistory: List<VersionHistory> = TemplatesSampleData.versionHistory,
    val selectedFilter: String = "All"
)

sealed interface TemplatesEvent {
    data class Filter(val label: String) : TemplatesEvent
    data object RequestTemplate : TemplatesEvent
    data class More(val name: String) : TemplatesEvent
}
