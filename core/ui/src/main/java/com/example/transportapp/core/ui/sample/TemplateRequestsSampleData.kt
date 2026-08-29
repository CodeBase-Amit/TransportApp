package com.example.transportapp.core.ui.sample

data class TemplateRequest(
    val id: String,
    val description: String,
    val status: String,
    val step: Int,
    val sentDate: String,
    val quotedDate: String? = null,
    val quotedAmount: String? = null,
    val amountNote: String? = null
)

data class PastRequest(
    val id: String,
    val description: String,
    val date: String,
    val amount: String,
    val status: String
)

/**
 * T30 Template requests demo data. UiState defaults all come from here so the
 * screen stays stateless and the Content composable never touches sample data.
 */
object TemplateRequestsSampleData {

    const val TITLE = "Template requests"
    const val OPEN_HEADING = "OPEN REQUESTS"
    const val PAST_HEADING = "PAST REQUESTS"
    const val NEW_REQUEST = "New request"
    const val APPROVE_PAY = "Approve and pay"
    const val SEE_PREVIEW = "See what they'll build"

    val stepLabels = listOf("Sent", "Recv", "Quote", "Apprv", "Inst")

    val openRequests: List<TemplateRequest> = listOf(
        TemplateRequest(
            id = "TR-2026-0037",
            description = "Bilty · 4 copies · from your own book",
            status = "QUOTED",
            step = 2,
            sentDate = "20 Aug",
            quotedDate = "22 Aug",
            quotedAmount = "₹2,500.00",
            amountNote = "Estimated"
        )
    )

    val pastRequests: List<PastRequest> = listOf(
        PastRequest("TR-2025-1102", "Bilty · 4 copies · GST layout", "Installed · 12 Oct 2025", "₹1,800.00", "INSTALLED"),
        PastRequest("TR-2025-0984", "Bilty · 4 copies · GST layout", "Installed · 05 Sep 2025", "₹2,150.00", "INSTALLED")
    )

    const val CAPTURE_TITLE = "New template capture"
    const val CAPTURE_STEP = "2/4"
    const val CAPTURE_BODY = "Capture all 4 corners of the vehicle chassis to generate an accurate dimensional template."
    const val CAPTURE_WARNING = "Blur the bottom-right corner — it shows a visible part number."
    const val CAPTURE_RETAKE = "Retake"
    const val CAPTURE_ADD_PHOTO = "Add photo"
    const val CAPTURE_SEND = "Send for checking"
}
