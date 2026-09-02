package com.example.transportapp.feature.masters.screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.masters.MastersRepository
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
 * T19 — Master editor (Phase2.md S3). Save writes PARTY_E + an outbox row; Delete is
 * refused with the §18.3 MASTER_IN_USE copy when bilties or rate rows reference the party.
 * S19: every field writes through to [savedStateHandle] — a half-edited party survives
 * process death (the editor re-opens with the draft, not the stored record).
 */
@HiltViewModel
class MasterEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val mastersRepository: MastersRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val masterType: String = savedStateHandle["type"] ?: "party"
    private val masterId: String = savedStateHandle["id"] ?: "new"
    /** A draft saved into the handle beats the stored record on re-open. */
    private val hasDraft: Boolean = savedStateHandle.get<Boolean>("me_draft") == true

    private val _uiState = MutableStateFlow(MasterEditorUiState())
    val uiState: StateFlow<MasterEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (hasDraft) {
                partyLocalId = savedStateHandle.get<String>("me_local_id")
                _uiState.update { MasterEditorUiState.fromDraft(savedStateHandle) }
                return@launch
            }
            val detail = if (masterId == "new") null else mastersRepository.resolveParty(masterId)
            partyLocalId = detail?.localId
            _uiState.update { if (detail == null && masterId != "new") MasterEditorUiState.from(null) else MasterEditorUiState.from(detail) }
        }
    }

    fun onEvent(event: MasterEditorEvent) {
        when (event) {
            is MasterEditorEvent.ChangeName -> _uiState.update { it.copy(name = event.value) }.also { persistDraft() }
            is MasterEditorEvent.ChangeEmail -> _uiState.update { it.copy(email = event.value) }.also { persistDraft() }
            is MasterEditorEvent.ChangePhone -> _uiState.update { it.copy(phone = event.value) }.also { persistDraft() }
            is MasterEditorEvent.ChangeStreet -> _uiState.update { it.copy(street = event.value) }.also { persistDraft() }
            is MasterEditorEvent.ChangeStation -> _uiState.update { it.copy(station = event.value) }.also { persistDraft() }
            is MasterEditorEvent.ChangePincode -> _uiState.update { it.copy(pincode = event.value) }.also { persistDraft() }
            is MasterEditorEvent.ChangeGstin -> _uiState.update { it.copy(gstin = event.value, taxStatus = if (event.value.isBlank()) "" else "Verified active taxpayer") }.also { persistDraft() }
            is MasterEditorEvent.ChangeRoute -> _uiState.update { it.copy(usualRoute = event.value) }.also { persistDraft() }
            is MasterEditorEvent.ChangeRateCard -> _uiState.update { it.copy(rateCard = event.value) }.also { persistDraft() }
            is MasterEditorEvent.SelectType -> _uiState.update { it.copy(type = event.value) }.also { persistDraft() }
            is MasterEditorEvent.SelectPayment -> _uiState.update { it.copy(payment = event.value) }.also { persistDraft() }
            MasterEditorEvent.Save -> viewModelScope.launch { save() }
            MasterEditorEvent.Delete -> viewModelScope.launch { delete() }
        }
    }

    private fun persistDraft() {
        val s = _uiState.value
        savedStateHandle["me_draft"] = true
        savedStateHandle["me_local_id"] = partyLocalId
        savedStateHandle["me_name"] = s.name
        savedStateHandle["me_email"] = s.email
        savedStateHandle["me_phone"] = s.phone
        savedStateHandle["me_street"] = s.street
        savedStateHandle["me_station"] = s.station
        savedStateHandle["me_pincode"] = s.pincode
        savedStateHandle["me_gstin"] = s.gstin
        savedStateHandle["me_route"] = s.usualRoute
        savedStateHandle["me_type"] = s.type
        savedStateHandle["me_payment"] = s.payment
    }

    private suspend fun save() {
        val state = _uiState.value
        _uiState.update { it.copy(error = null) }
        val companyId = sessionRepository.session.first().companyId
        val result = mastersRepository.createOrUpdateParty(
            companyId = companyId,
            localId = if (state.isNew) null else partyLocalId,
            name = state.name,
            phone = state.phone,
            email = state.email,
            street = state.street,
            station = state.station,
            pincode = state.pincode,
            gstin = state.gstin,
            type = when (state.type) {
                "Consignor" -> "CONSIGNOR"
                "Consignee" -> "CONSIGNEE"
                else -> "BOTH"
            },
            usualRouteId = null,
            usualPaymentMode = when (state.payment) {
                "Paid" -> "PAID"
                "To Pay" -> "TOPAY"
                else -> "TBB"
            },
        )
        when (result) {
            is com.example.transportapp.core.common.Result.Success -> {
                partyLocalId = result.value
                // S19: the draft is committed — the handle copy is cleared so the next
                // open reloads the stored record.
                savedStateHandle["me_draft"] = false
                _uiState.update { it.copy(justSaved = true, isNew = false) }
            }
            is com.example.transportapp.core.common.Result.Failure ->
                _uiState.update { it.copy(error = result.message ?: result.code.name) }
        }
    }

    private suspend fun delete() {
        val id = partyLocalId ?: return
        when (val result = mastersRepository.deleteParty(id)) {
            is com.example.transportapp.core.common.Result.Success -> _uiState.update { it.copy(error = null, deleteMessage = "") }
            is com.example.transportapp.core.common.Result.Failure ->
                _uiState.update { it.copy(deleteMessage = result.message ?: result.code.name) }
        }
    }

    private var partyLocalId: String? = null
}
