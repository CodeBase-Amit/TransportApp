package com.example.transportapp.feature.consignment

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.feature.consignment.screen.SheetOption
import com.example.transportapp.feature.consignment.screen.StatusUpdateSheetContent
import com.example.transportapp.feature.consignment.screen.StatusUpdateSheetEvent
import com.example.transportapp.feature.consignment.screen.StatusUpdateSheetUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S27 regression for the S15 delivery path: the save gate in StatusUpdateSheetViewModel
 * refuses DELIVERED without `hasSignature`, and `hasSignature` is only ever set by the
 * SetSignature event. The screen kept the ink in a local variable and never dispatched —
 * so every delivery save failed with "Capture the consignee's signature" even after
 * signing. This test pins the dispatch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatusUpdateSheetSignatureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `signing the pad dispatches SetSignature with ink`() {
        val events = mutableListOf<StatusUpdateSheetEvent>()
        val delivered = SheetOption(
            target = ConsignmentStatus.DELIVERED,
            label = "Delivered — POD captured",
            detail = "Legal next step from the current status",
        )
        composeRule.setContent {
            TransportAppTheme {
                StatusUpdateSheetContent(
                    state = StatusUpdateSheetUiState(biltyNo = "B/2026/0001", selected = delivered),
                    biltyNo = "B/2026/0001",
                    onEvent = { events.add(it) },
                    onDismiss = {},
                    onSave = {},
                )
            }
        }

        // The POD block sits below the fold in the sheet's scroll — bring it into view
        // or touch injection lands on empty screen space and the pad never sees the drag.
        // The POD block sits below the fold in the sheet's scroll — bring it into view
        // or touch injection lands on empty screen space and the pad never sees the drag.
        // The sheet is a dialog-boxed Column: match its corner shape, not the two app windows.
        composeRule.onNode(hasScrollAction().and(hasAnyDescendant(hasTestTag("pod_signature_pad"))))
            .performScrollToNode(hasTestTag("pod_signature_pad"))
        composeRule.onNodeWithTag("pod_signature_pad").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertTrue(
            "the sheet must dispatch SetSignature(hasInk=true) when the pad is signed; " +
                "otherwise the VM's delivery gate can never pass. Events seen: ${events.joinToString { it.javaClass.simpleName }}",
            events.any { it is StatusUpdateSheetEvent.SetSignature && it.hasInk },
        )
    }
}
