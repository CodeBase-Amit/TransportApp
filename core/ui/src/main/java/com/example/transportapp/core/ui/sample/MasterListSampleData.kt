package com.example.transportapp.core.ui.sample

data class MasterListParty(
    val name: String,
    val detail: String,
    val initials: String,
    val isDuplicate: Boolean = false
)

/**
 * T18 Master list (Parties) demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object MasterListSampleData {

    const val TITLE = "Parties"
    const val ADD_LABEL = "Add party"
    const val DUPLICATE_BANNER = "Same phone number"
    const val DUPLICATE_ACTION = "Merge"

    const val DEFAULT_FILTER_INDEX = 0
    const val DEFAULT_SECTION_HEADER = "D"

    val filterOptions = listOf(
        "All 1,284",
        "Used this month 212",
        "Never used 64",
        "Possible duplicates 7"
    )

    val alphabet: List<String> = ('A'..'Z').map { it.toString() } + "#"

    val parties: List<MasterListParty> = SampleData.partiesList.map {
        MasterListParty(name = it.name, detail = it.detail, initials = it.initials, isDuplicate = it.isDuplicate)
    }
}
