package com.example.transportapp.feature.booking.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.core.ui.sample.BiltyCopyConfig
import com.example.transportapp.core.ui.sample.BiltyPaperData
import com.example.transportapp.core.ui.sample.BiltySampleData
import com.example.transportapp.data.transport.consignment.ConsignmentRepository
import com.example.transportapp.data.transport.consignment.BiltySnapshotPayload
import com.example.transportapp.data.transport.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * T6 reads the persisted DOC_SNAPSHOT (§8): the paper is a function of the frozen print
 * payload and the pinned template version — never of today's rate card. The copy pager
 * paginates the snapshot's copy_count with the §8 copy labels.
 */
data class BiltyPreviewUiState(
    val biltyNo: String = BiltySampleData.BILTY_NO,
    val copyCount: Int = 4,
    val copyConfigs: List<BiltyCopyConfig> = DEFAULT_COPY_CONFIGS,
    val grandTotalFormatted: String = BiltySampleData.paper.grandTotal,
    val paper: BiltyPaperData = BiltySampleData.paper,
    val templateVersion: String = "1",
    val contentHash: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    companion object {
        val DEFAULT_COPY_CONFIGS = listOf(
            BiltyCopyConfig("CONSIGNOR", "Consignor copy"),
            BiltyCopyConfig("CONSIGNEE", "Consignee copy"),
            BiltyCopyConfig("DRIVER", "Driver copy"),
            BiltyCopyConfig("OFFICE", "Office copy"),
        )
    }
}

/** Feature-side mapping: the frozen payload strings onto the paper struct the screen renders. */
internal fun BiltySnapshotPayload.toPaperData(): BiltyPaperData = BiltyPaperData(
    companyName = companyName,
    addressLine = addressLine,
    contactLine = contactLine,
    consignorName = consignorName,
    consignorContact = consignorContact,
    consignorGstin = consignorGstin,
    consignorAddress = consignorAddress,
    consigneeName = consigneeName,
    consigneeContact = consigneeContact,
    consigneeGstin = consigneeGstin,
    consigneeAddress = consigneeAddress,
    docNo = docNo,
    date = date,
    fromStation = fromStation,
    toStation = toStation,
    goodsHeaders = listOf("Pkg", "Description", "Actual Wt.", "Charged Wt.", "Rate", "Amount"),
    goodsValues = listOf(packages, goodsDescription, actualWeight, chargeableWeight, rate, freight),
    freight = freight,
    hamali = hamali,
    doorDelivery = doorDelivery,
    taxable = taxable,
    gst = gst,
    rounding = rounding,
    totalLabel = totalLabel,
    grandTotal = grandTotal,
    amountInWords = amountInWords,
    stamp = stamp,
    footer = footer,
)

@HiltViewModel
class BiltyPreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val consignmentRepository: ConsignmentRepository,
) : ViewModel() {

    private val biltyNo: String = checkNotNull(savedStateHandle["biltyNo"])

    private val _uiState = MutableStateFlow(BiltyPreviewUiState(biltyNo = biltyNo))
    val uiState: StateFlow<BiltyPreviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            val snapshot = consignmentRepository.snapshotByBiltyNo(session.companyId, biltyNo)
            _uiState.update { state ->
                when (snapshot) {
                    null -> state.copy(isLoading = false, error = "No bilty found for $biltyNo")
                    else -> state.copy(
                        biltyNo = snapshot.biltyNo,
                        copyCount = snapshot.copyCount,
                        paper = snapshot.payload.toPaperData(),
                        templateVersion = snapshot.templateVersion,
                        contentHash = snapshot.contentHash,
                        isLoading = false,
                        error = null,
                    )
                }
            }
        }
    }
}
