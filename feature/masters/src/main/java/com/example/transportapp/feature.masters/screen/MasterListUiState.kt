package com.example.transportapp.feature.masters.screen

import com.example.transportapp.core.ui.sample.MasterListParty
import com.example.transportapp.core.ui.sample.MasterListSampleData

data class MasterListUiState(
    val title: String = MasterListSampleData.TITLE,
    val filterOptions: List<String> = MasterListSampleData.filterOptions,
    val selectedFilterIndex: Int = MasterListSampleData.DEFAULT_FILTER_INDEX,
    val alphabet: List<String> = MasterListSampleData.alphabet,
    val selectedLetterIndex: Int? = null,
    val sectionHeader: String = MasterListSampleData.DEFAULT_SECTION_HEADER,
    val parties: List<MasterListParty> = MasterListSampleData.parties,
    val duplicateBanner: String = MasterListSampleData.DUPLICATE_BANNER,
    val duplicateAction: String = MasterListSampleData.DUPLICATE_ACTION,
    val addLabel: String = MasterListSampleData.ADD_LABEL
)

sealed interface MasterListEvent {
    data class SelectFilter(val index: Int) : MasterListEvent
    data class SelectLetter(val index: Int) : MasterListEvent
    data object MergeDuplicates : MasterListEvent
}
