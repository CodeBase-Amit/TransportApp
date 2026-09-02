package com.example.transportapp.feature.consignment.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.session.SessionRepository
import com.example.transportapp.data.transport.tracking.NewStatusEvent
import com.example.transportapp.data.transport.tracking.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * T9's real save (Phase2.md S8): the sheet offers only the §7.1-legal continuations, the
 * hold path carries its reason and ≥10-character remark, and the save appends the event
 * with the projection advancing with it (D1). S19: the Camera/Gallery tiles go through the
 * real Photo Picker; the picked image is compressed into app files and rides the POD row.
 */
@HiltViewModel
class StatusUpdateSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val statusRepository: StatusRepository,
    private val photoImporter: com.example.transportapp.data.transport.tracking.PhotoImporter,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {

    private val biltyNo: String = checkNotNull(savedStateHandle["biltyNo"])

    /** The signature PNG's file ref, written by the sheet before SaveWithSignature arrives. */
    private val pendingSignatureRef = MutableStateFlow<String?>(null)

    /** S19: the POD photo's file ref, imported from the picker before the save. */
    private val pendingPhotoRef = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(StatusUpdateSheetUiState(biltyNo = biltyNo))
    val uiState: StateFlow<StatusUpdateSheetUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val session = sessionRepository.session.first()
            val current = statusRepository.currentStatus(biltyNo)
            val options = statusRepository.legalNext(biltyNo).map { target ->
                SheetOption(
                    target = target,
                    label = labelOf(target, session.branchName),
                    detail = "Legal next step from the current status",
                    holdPath = target == com.example.transportapp.domain.transport.ConsignmentStatus.HELD,
                )
            }
            _uiState.update {
                it.copy(
                    contextLine = current?.let { c -> "currently ${c.wording.lowercase()}" } ?: "",
                    options = options,
                    selected = options.firstOrNull(),
                )
            }
        }
    }

    private fun labelOf(target: com.example.transportapp.domain.transport.ConsignmentStatus, branchName: String): String = when (target) {
        com.example.transportapp.domain.transport.ConsignmentStatus.LOADED -> "Loaded — on the challan"
        com.example.transportapp.domain.transport.ConsignmentStatus.IN_TRANSIT -> "Departed — back in transit"
        com.example.transportapp.domain.transport.ConsignmentStatus.AT_HUB -> "At hub"
        com.example.transportapp.domain.transport.ConsignmentStatus.ARRIVED -> "Arrived at $branchName"
        com.example.transportapp.domain.transport.ConsignmentStatus.OUT_FOR_DELIVERY -> "Out for delivery"
        com.example.transportapp.domain.transport.ConsignmentStatus.DELIVERED -> "Delivered — POD captured"
        com.example.transportapp.domain.transport.ConsignmentStatus.HELD -> "Hold — exception"
        com.example.transportapp.domain.transport.ConsignmentStatus.RETURNED -> "Return to origin — RTO"
        else -> target.wording
    }

    fun onEvent(event: StatusUpdateSheetEvent) {
        when (event) {
            is StatusUpdateSheetEvent.SelectOption -> _uiState.update { it.copy(selected = event.option) }
            is StatusUpdateSheetEvent.SelectHoldReason -> _uiState.update { it.copy(holdReason = event.reason) }
            is StatusUpdateSheetEvent.ChangeRemark -> _uiState.update { it.copy(remark = event.value) }
            is StatusUpdateSheetEvent.ChangeLocation -> _uiState.update { it.copy(location = event.value) }
            is StatusUpdateSheetEvent.ChangeConsigneeName -> _uiState.update { it.copy(consigneeName = event.value, error = null) }
            is StatusUpdateSheetEvent.SetSignature -> _uiState.update { it.copy(hasSignature = event.hasInk) }
            StatusUpdateSheetEvent.ClearSignature -> _uiState.update { it.copy(hasSignature = false, signatureClearSignal = it.signatureClearSignal + 1) }
            StatusUpdateSheetEvent.UseMyLocation -> _uiState.update { it.copy(location = "Current town") }
            // S19: the picked image is copied + compressed off the UI thread; PHOTO_QUALITY
            // copy answers when the provider stream cannot be read.
            is StatusUpdateSheetEvent.PhotoPicked -> viewModelScope.launch {
                val imported = photoImporter.importToAppFiles(event.uri, "attachments")
                if (imported == null) {
                    _uiState.update { it.copy(error = "That photo could not be read. Try another one.") }
                } else {
                    pendingPhotoRef.value = imported.first
                    _uiState.update { it.copy(photoAttached = true) }
                }
            }
            StatusUpdateSheetEvent.RemovePhoto -> _uiState.update {
                pendingPhotoRef.value = null
                it.copy(photoAttached = false)
            }
            is StatusUpdateSheetEvent.SaveWithSignature -> {
                pendingSignatureRef.value = event.fileRef
                save()
            }
            StatusUpdateSheetEvent.Save -> save()
        }
    }

    private fun save() {
        val state = _uiState.value
        val selected = state.selected ?: return
        if (state.isSaving) return
        if (selected.holdPath && state.remark.length < 10) {
            _uiState.update { it.copy(error = "A hold needs a remark of at least ten characters") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            // S15: a delivery captures the POD first — consignee name plus the signed pad,
            // exported to a PNG in app files. The §7.1 gate then sees a real POD row.
            if (selected.target == com.example.transportapp.domain.transport.ConsignmentStatus.DELIVERED && !state.hasSignature) {
                _uiState.update { it.copy(isSaving = false, error = "Capture the consignee's signature before marking delivered") }
                return@launch
            }
            if (selected.target == com.example.transportapp.domain.transport.ConsignmentStatus.DELIVERED) {
                val signatureRef = pendingSignatureRef.value
                    ?: return@launch.also { _uiState.update { s -> s.copy(isSaving = false, error = "Capture the consignee's signature before marking delivered") } }
                val podResult = statusRepository.recordPod(
                    biltyNo = biltyNo,
                    consigneeName = state.consigneeName.ifBlank { "Consignee" },
                    signatureRef = signatureRef,
                    photoRef = pendingPhotoRef.value,
                    remarks = state.remark.takeIf { it.isNotBlank() },
                    now = System.currentTimeMillis(),
                )
                if (podResult.isFailure()) {
                    val failure = podResult as com.example.transportapp.core.common.Result.Failure
                    _uiState.update { it.copy(isSaving = false, error = failure.message ?: failure.code.name) }
                    return@launch
                }
            }
            val result = statusRepository.append(
                NewStatusEvent(
                    biltyNo = biltyNo,
                    eventType = selected.target.name,
                    location = state.location.takeIf { it.isNotBlank() },
                    remark = state.remark.takeIf { it.isNotBlank() },
                    reasonCode = if (selected.holdPath) state.holdReason.code else null,
                ),
                System.currentTimeMillis(),
            )
            val failure = result as? com.example.transportapp.core.common.Result.Failure
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = failure?.message ?: failure?.code?.name,
                    saved = result.isSuccess(),
                )
            }
        }
    }
}
