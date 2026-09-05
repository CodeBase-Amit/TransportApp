package com.example.transportapp.feature.templates.screen

import androidx.compose.runtime.Stable
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
 * T30 template requests (S18): labels inline, request lists default EMPTY — §15 is the
 * online tier, so a real install starts with none in flight (§5 decoupling).
 */
@Stable
data class TemplateRequestsUiState(
    val title: String = "Template requests",
    val openHeading: String = "OPEN REQUESTS",
    val pastHeading: String = "PAST REQUESTS",
    val newRequest: String = "New request",
    val approvePay: String = "Approve and pay",
    val seePreview: String = "See what they'll build",
    val stepLabels: List<String> = listOf("Sent", "Recv", "Quote", "Apprv", "Inst"),
    val openRequests: List<TemplateRequest> = emptyList(),
    val pastRequests: List<PastRequest> = emptyList(),
    val captureTitle: String = "New template capture",
    val captureStep: String = "2/4",
    val captureBody: String = "Capture all 4 corners of the vehicle chassis to generate an accurate dimensional template.",
    val captureWarning: String = "Blur the bottom-right corner — it shows a visible part number.",
    val captureRetake: String = "Retake",
    val captureAddPhoto: String = "Add photo",
    val captureSend: String = "Send for checking",
    val showCapture: Boolean = false
)

sealed interface TemplateRequestsEvent {
    data object NewRequest : TemplateRequestsEvent
    data object ApprovePay : TemplateRequestsEvent
    data object Preview : TemplateRequestsEvent
    data object OpenCapture : TemplateRequestsEvent
    data object CloseCapture : TemplateRequestsEvent
    data object Retake : TemplateRequestsEvent
    data object AddPhoto : TemplateRequestsEvent
    data object SendForChecking : TemplateRequestsEvent
}
