package com.example.transportapp.feature.reports.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.common.formatIndianGrouping
import com.example.transportapp.core.ui.ErrorCopy
import com.example.transportapp.data.transport.reports.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * T22 — the report viewer (§B22): the freight register as the widest report, first column
 * frozen, the totals band pinned, and CSV export straight from the same query.
 */
@HiltViewModel
class ReportViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reportsRepository: ReportsRepository,
) : ViewModel() {

    private val reportId: String = savedStateHandle.get<String>("reportId").orEmpty()
    private val dateFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)

    private val _uiState = MutableStateFlow(ReportViewerUiState())
    val uiState: StateFlow<ReportViewerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val fy = now - 150L * 24 * 60 * 60 * 1000
            val (rows, totals) = reportsRepository.freightRegister(companyWide = true, from = fy, to = now)
            _uiState.update {
                it.copy(
                    subtitle = "${SimpleDateFormat("d MMM", Locale.ENGLISH).format(fy)} – " +
                        "${SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(now)} · All branches · ${totals.rows} rows",
                    rows = rows.map { row ->
                        RegisterRowUi(
                            bilty = row.biltyNo,
                            date = dateFormat.format(row.bookedAt),
                            consignor = row.consignor,
                            weight = "${row.weightKg} kg",
                            amount = formatIndianGrouping(row.totalPaise / 100) + "." + (row.totalPaise % 100).toString().padStart(2, '0'),
                            status = if (row.cancelled) "CANCELLED" else "OK",
                        )
                    },
                    totalLabel = "TOTAL · ${totals.rows}",
                    totalWeight = "${totals.weightKg} kg",
                    totalAmount = formatIndianGrouping(totals.totalPaise / 100) + "." + (totals.totalPaise % 100).toString().padStart(2, '0'),
                    loading = false,
                )
            }
        }
    }

    fun onEvent(event: ReportViewerEvent) {
        when (event) {
            is ReportViewerEvent.RemoveFilter -> _uiState.update { it.copy(filters = it.filters - event.filter) }
            ReportViewerEvent.ClearAll -> _uiState.update { it.copy(filters = emptyList()) }
            ReportViewerEvent.OpenFilters -> _uiState.update { it }
            ReportViewerEvent.ExportExcel -> exportCsv()
            ReportViewerEvent.ExportPdf -> _uiState.update {
                it.copy(notice = "PDF export ships with the online tier — use Export to CSV today")
            }
            ReportViewerEvent.DismissNotice -> _uiState.update { it.copy(notice = null) }
        }
    }

    private fun exportCsv() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val fy = now - 150L * 24 * 60 * 60 * 1000
            val csv = reportsRepository.freightRegisterCsv(companyWide = true, from = fy, to = now)
            val result = reportsRepository.buildCsvExport("freight-register", csv, now)
            when (result) {
                is Result.Success -> _uiState.update { it.copy(notice = "Saved ${result.value.name}") }
                is Result.Failure -> _uiState.update { it.copy(notice = ErrorCopy.resolve(result)) }
            }
        }
    }
}
