package com.example.transportapp.feature.consignment

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.core.ui.sample.RegisterListItem
import com.example.transportapp.data.transport.consignment.RegisterSummary
import com.example.transportapp.feature.consignment.screen.ChipKind
import com.example.transportapp.feature.consignment.screen.RegisterContent
import com.example.transportapp.feature.consignment.screen.RegisterEvent
import com.example.transportapp.feature.consignment.screen.RegisterUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T7 register UI tests (Spec §12: every §6 empty/error state). The two empty states are
 * distinct by design (Design.md T7): an empty register is not a filtered-out register —
 * one offers "Book a bilty", the other offers "Clear filters".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RegisterScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** A PagingSource that immediately exhausts — the empty-register fixture. */
    private class EmptyPagingSource : PagingSource<Int, RegisterListItem>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RegisterListItem> =
            LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        override fun getRefreshKey(state: PagingState<Int, RegisterListItem>): Int? = null
    }

    private fun setContent(
        state: RegisterUiState,
        onEvent: (RegisterEvent) -> Unit = {},
        onNewBilty: () -> Unit = {}
    ) {
        composeRule.setContent {
            TransportAppTheme {
                val items = Pager(PagingConfig(pageSize = 20, initialLoadSize = 20)) {
                    EmptyPagingSource()
                }.flow.collectAsLazyPagingItems()
                RegisterContent(
                    state = state,
                    items = items,
                    onEvent = onEvent,
                    onBack = {},
                    onDocketClick = {},
                    onNewBilty = onNewBilty,
                    onHome = {},
                    onVehicles = {}
                )
            }
        }
    }

    @Test
    fun emptyRegister_noRecordsAtAll_showsBookCta() {
        var ctaFired = false
        setContent(
            state = RegisterUiState(
                summary = RegisterSummary(matching = 0, packages = 0, amountPaise = 0),
                isEmptyRegister = true,
                isLoading = false,
            ),
            onNewBilty = { ctaFired = true }
        )
        composeRule.waitForIdle()

        composeRule.onNodeWithText("No bilties yet").assertIsDisplayed()
        composeRule.onNodeWithText("Book a bilty").performClick()
        composeRule.runOnIdle { assertTrue(ctaFired) }
        composeRule.onAllNodesWithText("No bilties match these filters").assertCountEquals(0)
    }

    @Test
    fun emptyRegister_withSelectedChip_showsClearFiltersInstead() {
        var clearFired = false
        setContent(
            state = RegisterUiState(
                summary = RegisterSummary(matching = 0, packages = 0, amountPaise = 0),
                chips = RegisterUiState.defaultChips(selected = setOf(ChipKind.DELIVERED)),
                isLoading = false,
            ),
            onEvent = { event -> if (event is RegisterEvent.ClearFilters) clearFired = true }
        )
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText("No bilties yet").assertCountEquals(0)
        composeRule.onNodeWithText("No bilties match these filters").assertIsDisplayed()
        composeRule.onNodeWithText("Clear filters").performClick()
        composeRule.runOnIdle { assertTrue(clearFired) }
    }
}
