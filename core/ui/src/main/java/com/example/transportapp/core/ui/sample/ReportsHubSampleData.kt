package com.example.transportapp.core.ui.sample

data class ReportItem(val label: String, val desc: String, val figure: String? = null)
data class ReportGroup(val heading: String, val reports: List<ReportItem>)

/**
 * T21 Reports hub demo data. UiState defaults all come from here so the screen
 * stays stateless and the Content composable never touches sample data.
 */
object ReportsHubSampleData {

    const val TITLE = "Reports"
    const val PERIOD = "1 Apr 2026 – 25 Aug 2026"
    const val SCOPE = "All branches"
    const val PERIOD_NOTE = "Every report below uses this period and branch."

    val groups = listOf(
        ReportGroup("HOW MUCH DID WE EARN", listOf(
            ReportItem("Freight register", "Total booked amount", "₹4,18,72,140.00"),
            ReportItem("Delivery register", "Completed trips", "₹3,92,10,000.00")
        )),
        ReportGroup("WHAT DO PEOPLE OWE US", listOf(
            ReportItem("Outstanding", "Pending payments", "₹62,14,900.00"),
            ReportItem("Overdue (90+ days)", "Critical collections", "₹12,40,000.00")
        )),
        ReportGroup("WHAT DID IT COST US", listOf(
            ReportItem("Lorry hire", "Market vehicles", "₹18,42,000.00"),
            ReportItem("Fuel expenses", "Diesel & AdBlue", "₹45,90,200.00")
        )),
        ReportGroup("WHAT THE DEPARTMENT WILL ASK", listOf(
            ReportItem("GST outward (GSTR-1)", "Tax liability", "₹22,10,500.00"),
            ReportItem("E-way bill register", "Part A & B details", "1,402 generated")
        ))
    )
}
