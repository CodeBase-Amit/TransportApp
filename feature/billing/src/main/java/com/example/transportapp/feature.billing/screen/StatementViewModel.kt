package com.example.transportapp.feature.billing.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.common.Result
import com.example.transportapp.core.ui.ErrorCopy
import com.example.transportapp.data.transport.billing.BillingRepository
import com.example.transportapp.data.transport.billing.Statement
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
 * T16 — the party statement of account (§12.3): opening balance, a chronological ledger of
 * bills, receipts and credit notes, a closing balance, and the 90-plus-day ageing called out.
 * Opening and closing stay pinned while the middle scrolls.
 */
@HiltViewModel
class StatementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val billingRepository: BillingRepository,
) : ViewModel() {

    private val partyId: String = savedStateHandle.get<String>("partyId").orEmpty()

    private val _uiState = MutableStateFlow(StatementUiState())
    val uiState: StateFlow<StatementUiState> = _uiState.asStateFlow()

    init {
        // The period control: this financial year to date (1 April → today).
        val now = Calendar.getInstance()
        val fyStart = Calendar.getInstance().apply {
            set(Calendar.MONTH, Calendar.APRIL)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (after(now)) add(Calendar.YEAR, -1)
        }
        viewModelScope.launch {
            val result = billingRepository.statement(partyId, fyStart.timeInMillis, now.timeInMillis, now.timeInMillis)
            when (result) {
                is Result.Success -> _uiState.update { it.toLoaded(result.value) }
                is Result.Failure -> _uiState.update { it.copy(loading = false, error = ErrorCopy.resolve(result)) }
            }
        }
    }

    fun onEvent(event: StatementEvent) {
        when (event) {
            StatementEvent.SendPdf -> Unit // exports land in S10 (§14); the share affordance stays.
        }
    }
}

private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)
private val dayFormat = SimpleDateFormat("d MMM", Locale.ENGLISH)

private fun drCr(paise: Long): String =
    Money(kotlin.math.abs(paise)).formatted() + if (paise >= 0) " Dr" else " Cr"

private fun StatementUiState.toLoaded(statement: Statement): StatementUiState = copy(
    party = statement.partyName,
    partySubtitle = "Indore · GSTIN ${statement.gstin ?: "—"}",
    period = "${dateFormat.format(statement.periodStart)} – ${dateFormat.format(statement.periodEnd)}",
    opening = drCr(statement.openingPaise),
    ledgerRows = statement.rows.map { row ->
        StatementLedgerRow(
            date = dayFormat.format(row.at),
            docNo = row.docNo,
            desc = row.description,
            debit = if (row.debitPaise > 0) Money(row.debitPaise).formatted() else "—",
            credit = if (row.creditPaise > 0) Money(row.creditPaise).formatted() else "—",
            balance = drCr(row.balancePaise),
        )
    },
    closing = drCr(statement.closingPaise),
    ofWhich = if (statement.over90Paise > 0) {
        "of which ${Money(statement.over90Paise).formatted()} is over 90 days old"
    } else {
        "nothing is over 90 days old"
    },
    loading = false,
)
