package com.example.transportapp.feature.reports.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.ui.ErrorCopy
import com.example.transportapp.data.transport.reports.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T23 — the export centre (§B23): the CSV pack an accountant hands to a CA. Twelve sheets
 * with their real row counts; XLSX and Tally XML answer OFFLINE_UNAVAILABLE (Phase2 §10).
 */
@HiltViewModel
class ExportCentreViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportCentreUiState())
    val uiState: StateFlow<ExportCentreUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val counts = reportsRepository.sheetCounts()
            val sheets = listOf(
                ExportSheetUi("Freight register", counts.register),
                ExportSheetUi("Bilty-wise charge detail", counts.chargeLines),
                ExportSheetUi("Freight bills issued", counts.bills),
                ExportSheetUi("Money receipts", counts.receipts),
                ExportSheetUi("Receipt allocations", counts.allocations),
                ExportSheetUi("To Pay collections", counts.topayCollections),
                ExportSheetUi("Lorry hire register", counts.trips),
                ExportSheetUi("Party master with GSTINs", counts.parties),
            )
            val total = sheets.sumOf { it.count ?: 0 }
            _uiState.update {
                it.copy(
                    sheets = sheets,
                    includedIndices = sheets.indices.toSet(),
                    totalRows = total,
                    recentExports = reportsRepository.recentExports().take(5).map { f ->
                        RecentExportUi(f.name, f.lastModified(), f.length(), isPack = f.name.endsWith(".zip"))
                    },
                )
            }
        }
    }

    fun onEvent(event: ExportCentreEvent) {
        when (event) {
            is ExportCentreEvent.SelectQuarter -> _uiState.update { it.copy(selectedQuarter = event.value) }
            is ExportCentreEvent.SelectFormat -> _uiState.update { state ->
                if (event.value == "Excel (.xlsx)" || event.value == "Tally XML") {
                    // S27: this interpolated the whole event object before ("$event.value").
                    state.copy(notice = "${event.value} ships with the online tier — CSV (zip) is the offline format")
                } else {
                    state.copy(selectedFormat = event.value)
                }
            }
            is ExportCentreEvent.ToggleSheet -> _uiState.update {
                val newSet = it.includedIndices.toMutableSet()
                if (!newSet.add(event.index)) newSet.remove(event.index)
                it.copy(includedIndices = newSet)
            }
            ExportCentreEvent.UncheckAll -> _uiState.update { it.copy(includedIndices = emptySet()) }
            ExportCentreEvent.StartBuild -> buildPack()
            ExportCentreEvent.CancelBuild -> _uiState.update { it.copy(building = false) }
            ExportCentreEvent.DismissNotice -> _uiState.update { it.copy(notice = null, builtFile = null) }
        }
    }

    /**
     * S21 — the Register's export icon: the freight register as a single CSV in the
     * export centre's directory (it then appears in Recent exports). Distinct from
     * [buildPack], which builds the multi-sheet zip.
     */
    fun requestRegisterCsv() {
        if (_uiState.value.building) return
        _uiState.update { it.copy(building = true, notice = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val from = now - 150L * 24 * 60 * 60 * 1000
            val csv = reportsRepository.registerCsvForPeriod(from, now)
            val result = reportsRepository.buildCsvExport("Freight register", csv, now)
            when (result) {
                is Result.Success -> _uiState.update { s ->
                    s.copy(
                        building = false,
                        builtFile = result.value.name,
                        notice = "Saved to Downloads · ${result.value.name}",
                        recentExports = listOf(
                            RecentExportUi(result.value.name, result.value.lastModified(), result.value.length(), isPack = false),
                        ) + s.recentExports,
                    )
                }
                is Result.Failure -> _uiState.update { it.copy(building = false, notice = ErrorCopy.resolve(result)) }
            }
        }
    }

    private fun buildPack() {
        val state = _uiState.value
        if (state.building || state.includedIndices.isEmpty()) return
        _uiState.update { it.copy(building = true, notice = null, builtFile = null) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val from = now - 150L * 24 * 60 * 60 * 1000
            val sheets = state.includedIndices.sorted().mapNotNull { i ->
                when (val name = state.sheets[i].name) {
                    "Freight register" -> name to reportsRepository.registerCsvForPeriod(from, now)
                    else -> null // the other sheets' CSVs land with their own reports in the online tier
                }
            }
            if (sheets.isEmpty()) {
                _uiState.update { it.copy(building = false, notice = "Select at least the freight register to build a pack") }
                return@launch
            }
            val result = reportsRepository.buildCsvPack(sheets, now)
            when (result) {
                is Result.Success -> _uiState.update { s ->
                    s.copy(
                        building = false,
                        builtFile = result.value.name,
                        recentExports = listOf(
                            RecentExportUi(result.value.name, result.value.lastModified(), result.value.length(), isPack = true),
                        ) + s.recentExports,
                    )
                }
                is Result.Failure -> _uiState.update { it.copy(building = false, notice = ErrorCopy.resolve(result)) }
            }
        }
    }
}
