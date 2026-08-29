package com.example.transportapp.feature.templates.screen

import com.example.transportapp.core.ui.sample.PastRequest
import com.example.transportapp.core.ui.sample.TemplateRequest
import com.example.transportapp.core.ui.sample.TemplateRequestsSampleData

data class TemplateRequestsUiState(
    val title: String = TemplateRequestsSampleData.TITLE,
    val openHeading: String = TemplateRequestsSampleData.OPEN_HEADING,
    val pastHeading: String = TemplateRequestsSampleData.PAST_HEADING,
    val newRequest: String = TemplateRequestsSampleData.NEW_REQUEST,
    val approvePay: String = TemplateRequestsSampleData.APPROVE_PAY,
    val seePreview: String = TemplateRequestsSampleData.SEE_PREVIEW,
    val stepLabels: List<String> = TemplateRequestsSampleData.stepLabels,
    val openRequests: List<TemplateRequest> = TemplateRequestsSampleData.openRequests,
    val pastRequests: List<PastRequest> = TemplateRequestsSampleData.pastRequests,
    val captureTitle: String = TemplateRequestsSampleData.CAPTURE_TITLE,
    val captureStep: String = TemplateRequestsSampleData.CAPTURE_STEP,
    val captureBody: String = TemplateRequestsSampleData.CAPTURE_BODY,
    val captureWarning: String = TemplateRequestsSampleData.CAPTURE_WARNING,
    val captureRetake: String = TemplateRequestsSampleData.CAPTURE_RETAKE,
    val captureAddPhoto: String = TemplateRequestsSampleData.CAPTURE_ADD_PHOTO,
    val captureSend: String = TemplateRequestsSampleData.CAPTURE_SEND,
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
