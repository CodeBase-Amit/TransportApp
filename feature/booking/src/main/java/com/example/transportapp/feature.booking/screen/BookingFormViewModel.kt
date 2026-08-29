package com.example.transportapp.feature.booking.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.common.Money
import com.example.transportapp.core.ui.sample.BookingFormSampleData
import com.example.transportapp.core.ui.sample.ChargeLine
import com.example.transportapp.core.ui.sample.Party
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookingFormViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BookingFormUiState())
    val uiState: StateFlow<BookingFormUiState> = _uiState.asStateFlow()

    fun onEvent(event: BookingFormEvent) {
        when (event) {
            is BookingFormEvent.SelectConsignor -> _uiState.update {
                it.copy(consignor = event.party, isSearchingConsignor = false, searchQuery = "", searchResults = emptyList())
            }
            is BookingFormEvent.SelectConsignee -> _uiState.update {
                it.copy(consignee = event.party, isSearchingConsignee = false, searchQuery = "", searchResults = emptyList())
            }
            BookingFormEvent.ClearConsignor -> _uiState.update { it.copy(consignor = null) }
            BookingFormEvent.ClearConsignee -> _uiState.update { it.copy(consignee = null) }
            is BookingFormEvent.SearchConsignor -> {
                val results = if (event.query.length >= 2) {
                    listOf(BookingFormSampleData.deepakSteel, Party("Deepak Hardware", "+91 98765 43210", "Dewas", "23AACDH1234K1Z4", 3),
                        Party("Deep Enterprises", "+91 94250 12345", "Indore", "23AACDE5678K1Z4", 1))
                        .filter { it.name.contains(event.query, ignoreCase = true) || it.phone.contains(event.query) }
                        .take(3)
                } else emptyList()
                _uiState.update { it.copy(isSearchingConsignor = true, searchQuery = event.query, searchResults = results) }
            }
            is BookingFormEvent.SearchConsignee -> {
                val results = if (event.query.length >= 2) {
                    listOf(BookingFormSampleData.nashikHardware, Party("Nashik Cement Depot", "+91 98200 12345", "Nashik", "27AACNC1234K1Z8", 7))
                        .filter { it.name.contains(event.query, ignoreCase = true) }
                        .take(3)
                } else emptyList()
                _uiState.update { it.copy(isSearchingConsignee = true, searchQuery = event.query, searchResults = results) }
            }
            is BookingFormEvent.ChangePackages -> _uiState.update {
                val filtered = event.value.filter { it.isDigit() }
                it.copy(packages = filtered, charges = computeCharges(filtered, it.actualWeightKg))
            }
            is BookingFormEvent.ChangeWeight -> _uiState.update {
                val filtered = event.value.filter { it.isDigit() }
                val weightError = if (filtered.toIntOrNull() ?: 0 > 9000) "Weight can't be more than the vehicle's 9,000 kg capacity." else null
                it.copy(actualWeightKg = filtered, weightError = weightError, charges = computeCharges(it.packages, filtered))
            }
            is BookingFormEvent.ChangePaymentMode -> _uiState.update {
                it.copy(paymentMode = event.mode)
            }
            is BookingFormEvent.ChangeRisk -> _uiState.update { it.copy(risk = event.risk) }
            is BookingFormEvent.ChangeDelivery -> _uiState.update { it.copy(delivery = event.delivery) }
            BookingFormEvent.ToggleMoreDetails -> _uiState.update { it.copy(showMoreDetails = !it.showMoreDetails) }
            is BookingFormEvent.RemoveCharge -> _uiState.update {
                val newCharges = it.charges.toMutableList().apply { removeAt(event.index) }
                it.copy(charges = newCharges, taxable = recalcTaxable(newCharges), grandTotal = recalcTotal(newCharges))
            }
            BookingFormEvent.Submit -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true) }
                    // Navigate to preview — handled by the screen
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun computeCharges(packages: String, weight: String): List<ChargeLine> {
        val pkg = packages.toIntOrNull() ?: 12
        val wt = weight.toIntOrNull() ?: 780
        val freight = Money.fromRupees((wt * 450).toLong() / 100) // 4.50 * wt
        val hamali = Money.fromRupees((pkg * 8).toLong())
        val door = Money.fromRupees(150)
        val taxable = freight + hamali + door
        val gst = (taxable * 5) / 100
        val rounding = Money.fromRupees(0, 20)
        val total = taxable + gst + rounding
        return listOf(
            ChargeLine("Freight", freight, "$wt kg × 4.50", isComputed = true),
            ChargeLine("Hamali", hamali, "$pkg × 8.00", isRemovable = true),
            ChargeLine("Door delivery", door, "fixed", isRemovable = true)
        )
    }

    private fun recalcTaxable(charges: List<ChargeLine>): Money = charges.fold(Money.ZERO) { acc, c -> acc + c.amount }
    private fun recalcTotal(charges: List<ChargeLine>): Money {
        val taxable = recalcTaxable(charges)
        val gst = (taxable * 5) / 100
        val rounding = Money.fromRupees(0, 20)
        return taxable + gst + rounding
    }
}