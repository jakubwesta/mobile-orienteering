package com.mobileorienteering

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mobileorienteering.InstrumentedTestSupport.loginAsGuest
import com.mobileorienteering.InstrumentedTestSupport.navigateToMapTab
import com.mobileorienteering.InstrumentedTestSupport.openAddCheckpointDialog
import com.mobileorienteering.InstrumentedTestSupport.saveRouteAndWaitForLibrary
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MapCreationDeletionTest {

    @get:Rule(order = 0)
    val environmentRule = TestEnvironmentRule()

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun createMap_thenDeleteMap_updatesLibrary() {
        val mapName = "UI Test Route ${System.currentTimeMillis()}"

        with(composeTestRule) {
            loginAsGuest()
            navigateToMapTab()
            openAddCheckpointDialog()

            onNodeWithText("Add").performClick()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Control points").fetchSemanticsNodes().isNotEmpty()
            }

            expandCheckpointBottomSheet()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithTag("map_save_route").fetchSemanticsNodes().isNotEmpty()
            }

            onNodeWithTag("map_save_route").performClick()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithTag("map_route_name").fetchSemanticsNodes().isNotEmpty()
            }

            saveRouteAndWaitForLibrary(mapName)

            onNodeWithText(mapName).assertIsDisplayed()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Delete").performClick()

            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithText("Delete Map").fetchSemanticsNodes().isNotEmpty()
            }

            onAllNodesWithText("Delete")
                .filter(hasClickAction())
                .onFirst()
                .performClick()

            waitUntil(timeoutMillis = 20_000) {
                onAllNodesWithText(mapName).fetchSemanticsNodes().isEmpty()
            }

            onNodeWithText("No maps yet").assertIsDisplayed()
        }
    }

    private fun ComposeTestRule.expandCheckpointBottomSheet() {
        repeat(3) {
            onRoot().performTouchInput {
                swipeUp(startY = bottom * 0.85f, endY = bottom * 0.35f, durationMillis = 300)
            }
        }
    }
}
