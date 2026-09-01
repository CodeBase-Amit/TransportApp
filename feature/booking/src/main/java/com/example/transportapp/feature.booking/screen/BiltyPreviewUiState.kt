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
import com.example.transportapp.core.ui.PrintStatus
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
    private val documentRepository: com.example.transportapp.data.transport.documents.DocumentRepository,
) : ViewModel() {

    private val biltyNo: String = checkNotNull(savedStateHandle["biltyNo"])

    private val _uiState = MutableStateFlow(BiltyPreviewUiState(biltyNo = biltyNo))
    val uiState: StateFlow<BiltyPreviewUiState> = _uiState.asStateFlow()

    private val _printStatus = MutableStateFlow<PrintStatus>(PrintStatus.Idle)
    val printStatus: StateFlow<PrintStatus> = _printStatus.asStateFlow()

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

    /**
     * Print the four-copy document: the §8 copy labels paginate one paginated HTML through
     * the pinned template (S13). The system print dialog takes the rendered bytes (§9.8's
     * byte path); rendering errors surface through [PrintStatus].
     */
    fun print() {
        if (_printStatus.value is PrintStatus.Rendering) return
        _printStatus.value = PrintStatus.Rendering("Rendering the four copies…")
        viewModelScope.launch {
            val labels = documentRepository.copyLabels(biltyNo)
            when (val result = documentRepository.renderBilty(biltyNo, labels)) {
                is com.example.transportapp.core.common.Result.Success -> {
                    _printStatus.value = PrintStatus.Idle
                    documentRepository.print(result.value)
                }
                is com.example.transportapp.core.common.Result.Failure ->
                    _printStatus.value = PrintStatus.Error(result.message ?: "The document could not be printed")
            }
        }
    }

    /** Share the same rendered document through a content URI — the file name is the doc No. */
    fun share() {
        if (_printStatus.value is PrintStatus.Rendering) return
        _printStatus.value = PrintStatus.Rendering("Preparing to share…")
        viewModelScope.launch {
            val labels = documentRepository.copyLabels(biltyNo)
            when (val result = documentRepository.renderBilty(biltyNo, labels)) {
                is com.example.transportapp.core.common.Result.Success -> {
                    _printStatus.value = PrintStatus.Idle
                    documentRepository.share(result.value, "Share bilty ${result.value.fileName}")
                }
                is com.example.transportapp.core.common.Result.Failure ->
                    _printStatus.value = PrintStatus.Error(result.message ?: "The document could not be shared")
            }
        }
    }

    fun dismissPrintStatus() {
        _printStatus.value = PrintStatus.Idle
    }
}
