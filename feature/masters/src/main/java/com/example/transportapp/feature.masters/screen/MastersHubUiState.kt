package com.example.transportapp.feature.masters.screen

import com.example.transportapp.domain.transport.masters.MasterCounts

data class MasterGroup(
    val heading: String,
    val rows: List<Pair<String, String>>,
)

data class MastersHubUiState(
    val title: String = "Masters",
    val subtitle: String = "Reference data the booking form fills itself from. The better this is, the less anyone types.",
    val groups: List<MasterGroup> = emptyList(),
    val duplicateBanner: String = "",
    val duplicateAction: String = "Review them",
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    companion object {
        fun from(counts: MasterCounts, duplicateParties: Int) = MastersHubUiState(
            groups = listOf(
                MasterGroup(
                    "WHO AND WHERE",
                    listOf(
                        "Parties" to counts.parties.toLocale(),
                        "Stations" to counts.stations.toString(),
                        "Routes" to counts.routes.toString(),
                        "Branches" to counts.branches.toString(),
                    ),
                ),
                MasterGroup(
                    "WHAT AND HOW MUCH",
                    listOf(
                        "Goods types" to counts.goods.toString(),
                        "Charge heads" to counts.chargeHeads.toString(),
                        "Rate cards" to counts.rateCards.toString(),
                    ),
                ),
                MasterGroup(
                    "WHO CARRIES IT",
                    listOf(
                        "Vehicles" to counts.vehicles.toString(),
                        "Drivers" to counts.drivers.toString(),
                    ),
                ),
            ),
            duplicateBanner = if (duplicateParties > 0) {
                "$duplicateParties parties look like duplicates of another party."
            } else {
                ""
            },
            isLoading = false,
        )

        private fun Int.toLocale(): String = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(this)
    }
}

sealed interface MastersHubEvent {
    data object ReviewDuplicates : MastersHubEvent
}
