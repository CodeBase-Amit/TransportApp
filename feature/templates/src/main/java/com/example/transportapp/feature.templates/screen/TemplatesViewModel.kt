package com.example.transportapp.feature.templates.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transportapp.data.transport.templates.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * T29 — templates (Phase 3 S11): the list reads TEMPLATE_E through the repository; the
 * version history block shows every stored row of the default bilty template, because
 * versions are rows and a document keeps the version it was printed with (§17.2).
 */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplatesUiState())
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)

    init {
        templateRepository.observeTemplates()
            .onEach { summaries ->
                _uiState.update { state ->
                    val rows = summaries.map { s ->
                        val type = when {
                            s.templateKey.contains("bilty", ignoreCase = true) -> "Bilty"
                            s.templateKey.contains("challan", ignoreCase = true) -> "Challan"
                            s.templateKey.contains("receipt", ignoreCase = true) -> "Receipt"
                            else -> "Document"
                        }
                        TemplateRow(
                            templateKey = s.templateKey,
                            name = s.name,
                            type = type,
                            copies = if (type == "Bilty") "4 copies" else "1 copy",
                            paper = "A4",
                            version = "v${s.version}",
                            status = if (s.isActive) "active" else "superseded",
                            description = "${s.sectionCount} sections · engine schema v${s.schemaVersion}",
                            isDefault = s.isActive && type == "Bilty",
                            archived = !s.isActive,
                            tags = buildList {
                                if (s.visibility == "BUILT-IN") add("Built-in")
                                add("schema v${s.schemaVersion}")
                            },
                        )
                    }
                    val history = summaries
                        .filter { it.templateKey == "tpl-bilty-default" }
                        .sortedByDescending { it.version }
                        .map { s ->
                            VersionHistory(
                                version = "v${s.version}",
                                date = if (s.isActive) "active" else "superseded",
                                author = if (s.visibility == "BUILT-IN") "Engine" else "Company",
                                change = "Template installed as version ${s.version}",
                            )
                        }
                    state.copy(templates = rows, versionHistory = history)
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: TemplatesEvent) {
        when (event) {
            is TemplatesEvent.Filter -> _uiState.update { it.copy(selectedFilter = event.label) }
            TemplatesEvent.RequestTemplate -> _uiState.update { it }
            is TemplatesEvent.More -> _uiState.update { it }
        }
    }
}
