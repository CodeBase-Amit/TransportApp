package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.SavedStateHandle
import com.example.transportapp.data.transport.masters.MastersRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.masters.DuplicatePair
import com.example.transportapp.domain.transport.masters.PartyListRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T18 — Parties master list (Phase2.md S3): offline search as-you-type, A–Z rail,
 * filter chips with live counts, and the duplicate merge action.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MasterListViewModel @Inject constructor(
    private val mastersRepository: MastersRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterListUiState())
    val uiState: StateFlow<MasterListUiState> = _uiState.asStateFlow()

    private val query = MutableStateFlow("")
    private val filterIndex = MutableStateFlow(0)
    private val letterIndex = MutableStateFlow<Int?>(null)
    private var duplicatePair: DuplicatePair? = null

    init {
        viewModelScope.launch {
            val companyId = sessionRepository.session.first().companyId
            val partiesFlow = combine(
                query.debounce(150).distinctUntilChanged(),
                filterIndex,
                letterIndex,
            ) { q, f, l -> Triple(q, f, l) }
                .flatMapLatest { (q, f, l) ->
                    val letter = l?.let { uiStateAlphabet(it) } ?: ""
                    mastersRepository.observeParties(companyId, q, letter, duplicatesOnly = f == FILTER_DUPLICATES)
                        .map { rows -> decorate(rows, letter, f) }
                }
            val countsFlow = combine(
                mastersRepository.observeParties(companyId, "", "", duplicatesOnly = false),
                mastersRepository.observeDuplicateCount(companyId),
            ) { all, dupCount -> Triple(all.size, all.count { it.biltyCount > 0 }, dupCount) }

            combine(partiesFlow, countsFlow, mastersRepository.observeDuplicatePair(companyId)) { rows, counts, pair ->
                Triple(rows, counts, pair)
            }.collect { (rows, counts, pair) ->
                duplicatePair = pair
                _uiState.update {
                    it.copy(
                        parties = rows,
                        sectionHeader = rows.firstOrNull()?.name?.firstOrNull()?.uppercase() ?: "A",
                        filterOptions = listOf(
                            "All ${counts.first.toLocale()}",
                            "In use ${counts.second.toLocale()}",
                            "Never used ${(counts.first - counts.second).toLocale()}",
                            "Possible duplicates ${counts.third}",
                        ),
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun uiStateAlphabet(index: Int): String = _uiState.value.alphabet[index]

    private fun decorate(rows: List<PartyListRow>, letter: String, filter: Int): List<MasterListParty> =
        rows.map { row ->
            MasterListParty(
                localId = row.localId,
                name = row.name,
                detail = listOfNotNull(
                    row.station,
                    row.phone,
                    if (row.biltyCount > 0) "${row.biltyCount} bilties" else null,
                ).joinToString(" · "),
                initials = row.initials,
                isDuplicate = row.isDuplicate,
            )
        }

    fun onEvent(event: MasterListEvent) {
        when (event) {
            is MasterListEvent.SelectFilter -> {
                filterIndex.value = event.index
                _uiState.update { it.copy(selectedFilterIndex = event.index) }
            }
            is MasterListEvent.SelectLetter -> {
                letterIndex.value = if (letterIndex.value == event.index) null else event.index
                _uiState.update { it.copy(selectedLetterIndex = letterIndex.value) }
            }
            MasterListEvent.MergeDuplicates -> {
                val pair = duplicatePair ?: return
                viewModelScope.launch {
                    mastersRepository.mergeParties(pair.keepId, pair.mergeId)
                }
            }
            is MasterListEvent.SearchQuery -> {
                query.value = event.value
                _uiState.update { it.copy(query = event.value) }
            }
            MasterListEvent.ToggleSearch -> {
                _uiState.update {
                    it.copy(isSearching = !it.isSearching, query = if (it.isSearching) "" else it.query)
                }
            }
        }
    }

    private companion object {
        const val FILTER_DUPLICATES = 3
    }
}

private fun Int.toLocale(): String =
    java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(this)
