package com.example.transportapp.feature.masters.screen

import androidx.compose.runtime.Stable
/**
 * T18 — Master list (Parties). Rows come from PARTY_E via MastersRepository; the
 * duplicate flag is derived from parties sharing a phone number (Phase2.md S3).
 */
data class MasterListParty(
    val localId: String,
    val name: String,
    val detail: String,
    val initials: String,
    val isDuplicate: Boolean = false,
)

@Stable
data class MasterListUiState(
    val title: String = "Parties",
    val filterOptions: List<String> = listOf("All", "In use", "Never used", "Possible duplicates"),
    val selectedFilterIndex: Int = 0,
    val alphabet: List<String> = ('A'..'Z').map { it.toString() } + "#",
    val selectedLetterIndex: Int? = null,
    val sectionHeader: String = "A",
    val parties: List<MasterListParty> = emptyList(),
    val duplicateBanner: String = "Same phone number",
    val duplicateAction: String = "Merge",
    val addLabel: String = "Add party",
    val query: String = "",
    val isSearching: Boolean = false,
    val isLoading: Boolean = true,
)

sealed interface MasterListEvent {
    data class SelectFilter(val index: Int) : MasterListEvent
    data class SelectLetter(val index: Int) : MasterListEvent
    data object MergeDuplicates : MasterListEvent
    data class SearchQuery(val value: String) : MasterListEvent
    data object ToggleSearch : MasterListEvent
}
