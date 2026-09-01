package com.example.transportapp.feature.templates.screen

/** One T29 template card (Phase 3 S11: rows read TEMPLATE_E through the repository). */
data class TemplateRow(
    val templateKey: String,
    val name: String,
    val type: String,
    val copies: String,
    val paper: String,
    val version: String,
    val status: String,
    val description: String,
    val isDefault: Boolean = false,
    val neverPrinted: Boolean = false,
    val archived: Boolean = false,
    val tags: List<String> = emptyList(),
)

data class VersionHistory(
    val version: String,
    val date: String,
    val author: String,
    val change: String,
)

data class TemplatesUiState(
    val title: String = "Templates",
    val subtitle: String = "A template decides what a printed document looks like. Documents already issued keep the version they were printed with.",
    val requestTemplate: String = "Request template",
    val versionHistoryHeading: String = "Version history",
    val templates: List<TemplateRow> = emptyList(),
    val versionHistory: List<VersionHistory> = emptyList(),
    val selectedFilter: String = "All",
)

sealed interface TemplatesEvent {
    data class Filter(val label: String) : TemplatesEvent
    data object RequestTemplate : TemplatesEvent
    data class More(val name: String) : TemplatesEvent
}
