package com.example.transportapp.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.transportapp.core.designsystem.theme.TransportAppTheme
import com.example.transportapp.feature.settings.screen.AccountDataContent
import com.example.transportapp.feature.settings.screen.AccountDataUiState
import com.example.transportapp.feature.settings.screen.SettingsHubContent
import com.example.transportapp.feature.settings.screen.SettingsGroup
import com.example.transportapp.feature.settings.screen.SettingsHubUiState
import com.example.transportapp.feature.settings.screen.SettingsRow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * S17 UI tests: the settings hub routes every row (T24 wiring), and the debug screen-map
 * entry is a long-press on the diagnostics card in T31 (D53) — the gesture adb cannot
 * reproduce reliably, so it is verified here per Spec §12.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hub_rowClick_routesByLabel() {
        var routed: String? = null
        composeRule.setContent {
            TransportAppTheme {
                SettingsHubContent(
                    state = SettingsHubUiState(
                        identityName = "Mahesh Patidar",
                        groups = listOf(
                            SettingsGroup(
                                "YOUR COMPANY",
                                listOf(
                                    SettingsRow("building", "Company profile", "Shivshakti Roadlines"),
                                    SettingsRow("location", "Branches", "3 branches"),
                                    SettingsRow("numbers", "Numbering series", "7 series"),
                                )
                            )
                        )
                    ),
                    onEvent = {},
                    onBack = {},
                    onProfile = {},
                    onRowClick = { routed = it }
                )
            }
        }
        composeRule.onNodeWithText("Branches").performClick()
        composeRule.runOnIdle { assertTrue(routed == "Branches") }
    }

    @Test
    fun accountData_longPressOnDiagnosticsCard_opensScreenMap() {
        var screenMapRequested = false
        composeRule.setContent {
            TransportAppTheme {
                AccountDataContent(
                    state = AccountDataUiState(),
                    onEvent = {},
                    onBack = {},
                    onOpenScreenMap = { screenMapRequested = true }
                )
            }
        }
        composeRule.onNodeWithText("Records stored").assertIsDisplayed()
        composeRule.onNodeWithText("Records stored").performTouchInput {
            down(center)
            advanceEventTime(2_000)
            up()
        }
        composeRule.runOnIdle { assertTrue(screenMapRequested) }
    }
}
