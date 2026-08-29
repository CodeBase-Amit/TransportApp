package com.example.transportapp.feature.settings.screen

import com.example.transportapp.core.ui.sample.BranchRow
import com.example.transportapp.core.ui.sample.BranchesSampleData

data class BranchesUiState(
    val title: String = BranchesSampleData.TITLE,
    val subtitle: String = BranchesSampleData.SUBTITLE,
    val addBranch: String = BranchesSampleData.ADD_BRANCH,
    val headOfficeChip: String = BranchesSampleData.HEAD_OFFICE_CHIP,
    val branches: List<BranchRow> = BranchesSampleData.rows
)

sealed interface BranchesEvent {
    data object AddBranch : BranchesEvent
    data object BranchMore : BranchesEvent
}
