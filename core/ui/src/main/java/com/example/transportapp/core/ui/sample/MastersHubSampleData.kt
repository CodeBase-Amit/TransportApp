package com.example.transportapp.core.ui.sample

data class MasterGroup(val heading: String, val rows: List<Pair<String, String>>)

/**
 * T17 Masters hub demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object MastersHubSampleData {

    const val TITLE = "Masters"
    const val SUBTITLE = "Reference data the booking form fills itself from. The better this is, the less anyone types."
    const val DUPLICATE_BANNER = "7 parties look like duplicates of another party."
    const val DUPLICATE_ACTION = "Review them"

    val groups: List<MasterGroup> = SampleData.masterGroups.map {
        MasterGroup(it.heading, it.rows)
    }
}
