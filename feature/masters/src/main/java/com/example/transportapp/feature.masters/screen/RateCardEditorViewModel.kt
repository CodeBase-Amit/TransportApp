package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.data.transport.masters.MastersRepository
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.domain.transport.masters.RateRow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T20 — Rate card editor (Phase2.md S3). Rows are the party-scoped RATE_CARD_E rows
 * (§3: party-scoped beats company default); auto charges come from CHARGE_HEAD_E.
 */
@HiltViewModel
class RateCardEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mastersRepository: MastersRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val partyIdOrName: String = savedStateHandle["partyId"] ?: "deepak"

    private val _uiState = MutableStateFlow(RateCardEditorUiState())
    val uiState: StateFlow<RateCardEditorUiState> = _uiState.asStateFlow()

    /** S21: the add-rate dialog's rupee field. */
    private val _newRate = MutableStateFlow("")
    val newRate: StateFlow<String> = _newRate.asStateFlow()

    private var rows: List<RateRow> = emptyList()

    init {
        viewModelScope.launch {
            val party = mastersRepository.resolveParty(partyIdOrName)
            val companyId = sessionRepository.session.first().companyId
            rows = mastersRepository.rateRowsForParty(party?.localId ?: return@launch)
            val charges = mastersRepository.autoCharges(companyId)
            _uiState.update {
                it.copy(
                    subtitle = "${party?.name.orEmpty()} · Rate card 2026-27 · ${rows.size} rates",
                    ratesHeading = "Rates · ${rows.size}",
                    viewAll = "View all ${rows.size} rates",
                    rateRows = rows.take(VISIBLE_ROWS).map(::toRowState),
                    charges = charges.map { c -> RateChargeItem(c.label, c.displayValue ?: "", c.enabled) },
                    isLoading = false,
                )
            }
        }
    }

    private fun toRowState(row: RateRow) = RateCardRow(
        localId = row.localId,
        route = row.routeLabel,
        goods = row.goodsLabel,
        basis = basisLabel(row.basis),
        rate = formatMoney(row.ratePaise, row.basis),
        minQty = row.minQtyLabel.orEmpty(),
    )

    private fun basisLabel(basis: String) = when (basis) {
        "PER_KG" -> "Per kg"
        "PER_TONNE" -> "Per Ton"
        "PER_PACKAGE" -> "Per package"
        "PER_TRIP" -> "Per trip"
        else -> "Fixed"
    }

    private fun formatMoney(paise: Long, basis: String): String {
        val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(paise / 100.0)
        return if (basis == "PER_KG") String.format(Locale.US, "%.2f", paise / 100.0) else formatted
    }

    fun onEvent(event: RateCardEditorEvent) {
        when (event) {
            is RateCardEditorEvent.ToggleCharge -> _uiState.update {
                it.copy(charges = it.charges.mapIndexed { i, c -> if (i == event.index) c.copy(enabled = !c.enabled) else c })
            }
            // S21: AddRate really adds a row — it copies the party's existing basis/scope
            // and prices at the entered value once the dialog fires AddRateConfirmed.
            RateCardEditorEvent.AddRate -> _uiState.update { it.copy(showAddRate = true) }.also { _newRate.value = "" }
            RateCardEditorEvent.DismissAddRate -> _uiState.update { it.copy(showAddRate = false) }.also { _newRate.value = "" }
            is RateCardEditorEvent.ChangeNewRate -> _newRate.value = event.value.filter { ch -> ch.isDigit() }
            RateCardEditorEvent.ConfirmAddRate -> viewModelScope.launch {
                val paise = (_newRate.value.toLongOrNull() ?: 0L) * 100
                if (paise <= 0L) return@launch
                val session = sessionRepository.session.first()
                val party = mastersRepository.resolveParty(partyIdOrName) ?: return@launch
                val result = mastersRepository.addRateRow(session.companyId, party.localId, paise)
                when (result) {
                    is com.example.transportapp.core.common.Result.Success -> {
                        rows = mastersRepository.rateRowsForParty(party.localId)
                        _uiState.update {
                            it.copy(
                                showAddRate = false,
                                subtitle = "${party.name} · Rate card 2026-27 · ${rows.size} rates",
                                ratesHeading = "Rates · ${rows.size}",
                                viewAll = "View all ${rows.size} rates",
                                rateRows = rows.take(VISIBLE_ROWS).map(::toRowState),
                            )
                        }
                    }
                    is com.example.transportapp.core.common.Result.Failure ->
                        _uiState.update { it.copy(showAddRate = false) }
                }
            }
            RateCardEditorEvent.ViewAllRates -> _uiState.update {
                it.copy(showAllRates = !it.showAllRates, rateRows = if (it.showAllRates) rows.take(VISIBLE_ROWS).map(::toRowState) else rows.map(::toRowState))
            }
            RateCardEditorEvent.SaveRateCard -> viewModelScope.launch {
                rows.forEach { row -> mastersRepository.saveRateRow(row.localId, row.ratePaise) }
                _uiState.update { it.copy(justSaved = true) }
            }
        }
    }

    private companion object {
        const val VISIBLE_ROWS = 3
    }
}
