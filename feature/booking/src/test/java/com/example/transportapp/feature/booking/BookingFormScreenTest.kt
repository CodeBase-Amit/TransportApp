package com.example.transportapp.feature.booking

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assert
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.ui.sample.BookingFormSampleData
import com.example.transportapp.feature.booking.screen.BookingFormContent
import com.example.transportapp.feature.booking.screen.BookingFormEvent
import com.example.transportapp.feature.booking.screen.BookingFormUiState
import com.example.transportapp.domain.transport.PaymentMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T5 booking-form UI tests (Spec §12: booking validation). Drives [BookingFormContent]
 * with a scripted UiState and records the events it emits — the content is a pure
 * function of state per Spec §5, so this exercises the real composition without Hilt.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookingFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** State holder backed by snapshot state so reductions actually recompose the UI. */
    private class Script {
        var state by mutableStateOf(BookingFormUiState())
        val events = mutableListOf<BookingFormEvent>()
    }

    private fun setContent(script: Script) {
        composeRule.setContent {
            TransportAppTheme {
                BookingFormContent(
                    state = script.state,
                    onEvent = { event ->
                        // Reduce inline so multi-step interactions see the resulting state.
                        script.state = reduce(script.state, event)
                        script.events.add(event)
                    },
                    onClose = {},
                    onBookAndPrint = {},
                    onSetRate = {}
                )
            }
        }
    }

    /** The reducer rules the UI relies on, mirroring BookingFormViewModel. */
    private fun reduce(state: BookingFormUiState, event: BookingFormEvent): BookingFormUiState = when (event) {
        is BookingFormEvent.ChangeWeight -> {
            val filtered = event.value.filter { ch -> ch.isDigit() }
            state.copy(
                actualWeightKg = filtered,
                weightError = if ((filtered.toLongOrNull() ?: 0) > 9000L) {
                    "Weight can't be more than the vehicle's 9,000 kg capacity. Check the figure or split the consignment."
                } else null
            )
        }
        is BookingFormEvent.ChangePackages -> state.copy(packages = event.value.filter { it.isDigit() })
        is BookingFormEvent.ChangePaymentMode -> state.copy(paymentMode = event.mode)
        is BookingFormEvent.AddArticle -> state.copy(extraItems = state.extraItems + com.example.transportapp.feature.booking.screen.ArticleRow())
        is BookingFormEvent.RemoveArticle -> state.copy(extraItems = state.extraItems.filterIndexed { i, _ -> i != event.index })
        is BookingFormEvent.ChangeArticleDescription -> state.copy(extraItems = state.extraItems.mapIndexed { i, row -> if (i == event.index) row.copy(description = event.value) else row })
        is BookingFormEvent.ChangeArticlePackages -> state.copy(extraItems = state.extraItems.mapIndexed { i, row -> if (i == event.index) row.copy(packages = event.value.filter { ch -> ch.isDigit() }) else row })
        is BookingFormEvent.ChangeArticleWeight -> state.copy(extraItems = state.extraItems.mapIndexed { i, row -> if (i == event.index) row.copy(weightKg = event.value.filter { ch -> ch.isDigit() }) else row })
        is BookingFormEvent.ClearConsignor -> state.copy(consignor = null, isSearchingConsignor = false)
        is BookingFormEvent.StartConsignorSearch -> state.copy(isSearchingConsignor = true)
        is BookingFormEvent.SelectConsignor -> state.copy(consignor = event.party, isSearchingConsignor = false)
        else -> state
    }

    @Test
    fun weightOverVehicleCapacity_showsError_withCauseAndFix() {
        val script = Script()
        setContent(script)

        scrollToWeight()
        val field = composeRule.onNodeWithText("Actual weight")
        field.performTextClearance()
        field.performTextInput("12000")
        composeRule.waitForIdle()

        assertTrue(script.state.weightError != null)
        // The error renders below the field inside the same lazy item — bring it into view.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("9,000 kg", substring = true))
        composeRule.onNodeWithText("9,000 kg", substring = true).assertIsDisplayed()
    }

    @Test
    fun weightWithinCapacity_noErrorShown() {
        val script = Script()
        setContent(script)

        scrollToWeight()
        val field = composeRule.onNodeWithText("Actual weight")
        field.performTextClearance()
        field.performTextInput("780")
        composeRule.waitForIdle()

        assertTrue(script.state.weightError == null)
        composeRule.onAllNodesWithText("9,000 kg", substring = true).assertCountEquals(0)
    }

    @Test
    fun nonNumericWeightInput_isFilteredBeforeStateSeesIt() {
        val script = Script()
        setContent(script)

        scrollToWeight()
        val field = composeRule.onNodeWithText("Actual weight")
        field.performTextClearance()
        field.performTextInput("7a8b0")
        composeRule.waitForIdle()

        assertEquals("780", script.state.actualWeightKg)
    }

    @Test
    fun paymentModeSegment_tapSwitchesMode() {
        val script = Script()
        setContent(script)

        scrollToPayment()
        composeRule.onNodeWithText("Paid").performClick()
        composeRule.waitForIdle()

        assertEquals(PaymentMode.PAID, script.state.paymentMode)
    }

    /** The form is a LazyColumn — the weight and terms sections sit below the fold. */
    private fun scrollToWeight() {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Actual weight"))
    }

    private fun scrollToPayment() {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Paid"))
    }

    @Test
    fun grandTotalBar_showsAmountAndWords() {
        val script = Script()
        // S18: the form starts empty; the sticky bar renders whatever totals the state carries.
        script.state = BookingFormUiState(
            grandTotal = BookingFormSampleData.GRAND_TOTAL,
            amountInWords = BookingFormSampleData.AMOUNT_IN_WORDS,
        )
        setContent(script)

        // The sticky bar prints the rupee-marked total above the amount in words.
        composeRule.onNodeWithText(BookingFormSampleData.GRAND_TOTAL.formatted(), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(BookingFormSampleData.AMOUNT_IN_WORDS, substring = true).assertIsDisplayed()
    }

    @Test
    fun clearConsignor_showsTapToAdd_thenSearchField_rendersResults() {
        val script = Script()
        // S18: the form opens with both party cards empty — "Tap to add" is the entry point.
        setContent(script)

        composeRule.onAllNodesWithText("Tap to add").onFirst().assertIsDisplayed()

        // Tap the empty card: the search field enters search mode (S14 pickers).
        composeRule.onAllNodesWithText("Tap to add").onFirst().performClick()
        composeRule.waitForIdle()
        assertTrue(script.events.contains(BookingFormEvent.StartConsignorSearch))

        // The reducer enters search mode; the field renders and the result row is tappable.
        script.state = script.state.copy(
            isSearchingConsignor = true,
            searchResults = listOf(
                com.example.transportapp.core.ui.sample.Party(
                    id = "bench-party-1",
                    name = "Sharma Traders 0001",
                    phone = "+91 970000001",
                    station = "Indore",
                    gstin = "",
                ),
            ),
        )
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Sharma Traders 0001").onFirst().performClick()
        composeRule.waitForIdle()
        assertTrue(script.events.any { it is BookingFormEvent.SelectConsignor })
        assertEquals("Sharma Traders 0001", (script.events.last { it is BookingFormEvent.SelectConsignor } as BookingFormEvent.SelectConsignor).party.name)
    }

    @Test
    fun routePicker_rendersOptions_andSelectionEmitsRouteEvent() {
        val script = Script()
        script.state = script.state.copy(
            routeLabel = "Pick a route",
            showRoutePicker = true,
            routeOptions = listOf(
                "seed-route-indore-nashik" to "Indore → Nashik · 585 km · usually 2 days",
                "seed-route-indore-bhusawal" to "Indore → Bhusawal · 250 km · usually 1 day",
            ),
        )
        setContent(script)

        // Two-step scroll: bring the route card into composition first, then the chosen
        // row (LazyColumn doesn't compose items beyond the viewport's lookahead).
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Pick a route"))
        composeRule.waitForIdle()
        // Second step: the list is composed now, so the row is reachable.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Indore → Bhusawal", substring = true))
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Indore → Bhusawal · 250 km · usually 1 day").performClick()
        composeRule.waitForIdle()

        assertEquals(BookingFormEvent.SelectRoute("seed-route-indore-bhusawal"), script.events.last())
    }
    @Test
    fun articlesSection_addRow_editsAggregate_andEmitsEvents() {
        val script = Script()
        setContent(script)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Add article"))
        composeRule.onNodeWithText("Add article").performClick()
        composeRule.waitForIdle()

        // The reducer appends a row; fill it through the state (the content is a pure
        // function of state — the fields render what the reducer stored).
        script.state = script.state.copy(
            extraItems = listOf(
                com.example.transportapp.feature.booking.screen.ArticleRow(
                    description = "TMT bars", packages = "6", weightKg = "400",
                ),
            ),
        )
        composeRule.waitForIdle()

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Article 2 description"))
        composeRule.onNodeWithText("Article 2 description").assertIsDisplayed()
        composeRule.onNodeWithText("Remove").assertIsDisplayed()

        composeRule.onNodeWithText("Remove").performClick()
        composeRule.waitForIdle()
        assertTrue(script.events.contains(BookingFormEvent.RemoveArticle(0)))
    }
}
