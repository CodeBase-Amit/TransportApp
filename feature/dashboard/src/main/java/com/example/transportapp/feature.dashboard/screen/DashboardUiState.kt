package com.example.transportapp.feature.dashboard.screen

/** One §13 tile as the grid prints it. Visible=false tiles are role-hidden (§13). */
data class DashTile(
    val label: String,
    val value: String,
    val qualifier: String,
    val money: Boolean = false,
    val amberBar: Boolean = false,
)

data class DashException(
    val title: String,
    val body: String,
    val isLate: Boolean = false,
)

/**
 * T4 — Dashboard (§13): every tile is a projection over already-synced Room data, stamped
 * "as of" rather than pretending to be live. The exception strip is the only error-coloured
 * surface and it is dismissible per item.
 */
data class DashboardUiState(
    val companyInitials: String = "SR",
    val companyName: String = "",
    val branchName: String = "",
    val asOf: String = "",
    val exceptions: List<DashException> = emptyList(),
    val dismissedExceptions: Set<Int> = emptySet(),
    val tiles: List<DashTile> = emptyList(),
    val thisMonthFigures: List<Pair<String, String>> = emptyList(),
    val thisMonthDelta: String = "",
    val newBiltyLabel: String = "New bilty",
) {
    val visibleExceptions: List<DashException>
        get() = exceptions.filterIndexed { i, _ -> i !in dismissedExceptions }
}

sealed interface DashboardEvent {
    data class DismissException(val index: Int) : DashboardEvent
    data object Refresh : DashboardEvent
}
