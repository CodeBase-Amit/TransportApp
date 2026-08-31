package com.example.transportapp.feature.reports.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.reports.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * T21 — the reports hub (§B21): reports grouped by the question they answer, with cached
 * headline figures where they exist. Figures are projections over synced Room data.
 */
@HiltViewModel
class ReportsHubViewModel @Inject constructor(
    private val reportsRepository: ReportsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsHubUiState())
    val uiState: StateFlow<ReportsHubUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            val period = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(fyStart(now)) +
                " – " + SimpleDateFormat("d MMM yyyy", Locale.ENGLISH).format(now)
            val entries = reportsRepository.hubEntries(now)
            val groups = entries.groupBy { it.group }.map { (heading, rows) ->
                ReportGroupUi(
                    heading = heading,
                    reports = rows.map { ReportRowUi(it.id, it.label, it.description, it.headline) },
                )
            }
            _uiState.update { it.copy(period = period, groups = groups, loading = false) }
        }
    }

    fun onEvent(event: ReportsHubEvent) {
        when (event) {
            ReportsHubEvent.ChangePeriod -> _uiState.update { it }
        }
    }

    private fun fyStart(now: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.MONTH, Calendar.APRIL)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        if (cal.after(Calendar.getInstance().apply { timeInMillis = now })) cal.add(Calendar.YEAR, -1)
        return cal.timeInMillis
    }
}
