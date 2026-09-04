package com.example.transportapp.feature.settings.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.account.AccountDataRepository
import com.example.transportapp.data.transport.session.SessionRepository
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
 * T31 — Account and data (§B31): storage facts, the real OUTBOX queue read as sentences,
 * and sign-out. The destructive leave/delete blocks stay visual until the online tier.
 * All data comes from [AccountDataRepository] — a ViewModel never touches files (Spec §14).
 * S25: "Try now" forces a real drain + masters refresh through the repositories.
 */
@HiltViewModel
class AccountDataViewModel @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val sessionRepository: SessionRepository,
    private val outboxPush: com.example.transportapp.data.transport.sync.OutboxPush,
    private val mastersRefresher: com.example.transportapp.data.transport.masters.MastersRefresher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDataUiState())
    val uiState: StateFlow<AccountDataUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.ENGLISH)

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            // S25: Try now actually drains — push outbox rows, then pull masters — before
            // re-reading the queue so the UI reflects what just left the device.
            val pushed = when (val report = outboxPush.drain()) {
                is com.example.transportapp.core.common.Result.Success -> report.value.pushed
                is com.example.transportapp.core.common.Result.Failure -> 0
            }
            mastersRefresher.refreshAll()
            val data = accountDataRepository.phoneData()
            _uiState.update {
                it.copy(
                    records = data.records.toString(),
                    space = "${data.dbBytes / (1024 * 1024) + 1} MB",
                    waiting = if (data.queue.isEmpty()) "EVERYTHING IS SYNCED" else "WAITING TO SYNC · ${data.queue.size}",
                    syncQueue = data.queue.take(3).map { row ->
                        SyncQueueRowUi(
                            description = row.description,
                            atText = timeFormat.format(row.createdAt),
                            state = if (row.pending) "Pending" else "Syncing",
                        )
                    },
                    tryNow = "Sync now · $pushed sent",
                )
            }
        }
    }

    fun onEvent(event: AccountDataEvent) {
        when (event) {
            AccountDataEvent.TrySync -> refresh()
            AccountDataEvent.ClearCached -> _uiState.update { it }
            AccountDataEvent.SignOut -> viewModelScope.launch {
                sessionRepository.signOut()
                _uiState.update { it.copy(signedOut = true) }
            }
            AccountDataEvent.Leave -> _uiState.update { it }
            AccountDataEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true) }
            AccountDataEvent.CancelDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
            AccountDataEvent.ConfirmDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
        }
    }
}
