package com.mobileorienteering

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

object InstrumentedTestSupport {

    fun prepareTestEnvironment() {
        clearAuthState()
        disableAnimations()
        grantLocationPermissions()
    }

    fun clearAuthState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val datastoreDir = File(context.filesDir, "datastore")
        if (datastoreDir.exists()) {
            datastoreDir.listFiles()?.forEach { it.delete() }
        }

        context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    fun disableAnimations() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.executeShellCommand("settings put global window_animation_scale 0")
        automation.executeShellCommand("settings put global transition_animation_scale 0")
        automation.executeShellCommand("settings put global animator_duration_scale 0")
    }

    fun grantLocationPermissions() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        automation.grantRuntimePermission(packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            automation.grantRuntimePermission(packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun ComposeTestRule.dismissWelcomeIfNeeded() {
        if (!hasNodesWithText("Get Started")) return

        onNodeWithText("Get Started").performClick()
        waitUntil(timeoutMillis = 10_000) {
            !hasNodesWithText("Get Started")
        }
    }

    fun ComposeTestRule.loginAsGuest() {
        waitForIdle()

        if (isOnMainApp()) return

        dismissWelcomeIfNeeded()

        if (isOnMainApp()) return

        waitUntil(timeoutMillis = 15_000) {
            hasNodesWithText("Continue as Guest") ||
                hasNodesWithText("Log in") ||
                hasNodesWithText("Get Started")
        }

        dismissWelcomeIfNeeded()

        if (hasNodesWithText("Continue as Guest")) {
            onNodeWithText("Continue as Guest").performClick()
        }

        waitUntil(timeoutMillis = 15_000) {
            isOnMainApp()
        }
    }

    fun ComposeTestRule.navigateToMapTab(mapLoadTimeoutMillis: Long = 20_000L) {
        onNodeWithText("Map").performClick()
        waitUntil(timeoutMillis = 15_000) {
            hasNodesWithTag("map_add_checkpoint")
        }
        waitUntil(timeoutMillis = mapLoadTimeoutMillis) {
            hasNodesWithTag("map_ready")
        }
    }

    fun ComposeTestRule.openAddCheckpointDialog(timeoutMillis: Long = 10_000) {
        onNodeWithTag("map_add_checkpoint").performClick()
        waitUntil(timeoutMillis) {
            hasNodesWithText("Add control point")
        }
    }

    fun ComposeTestRule.ensureLoggedIn() {
        if (!hasNodesWithText("Continue as Guest")) return

        onNodeWithText("Continue as Guest").performClick()
        waitUntil(timeoutMillis = 15_000) {
            isOnMainApp()
        }
    }

    fun ComposeTestRule.saveRouteAndWaitForLibrary(mapName: String) {
        onNodeWithTag("map_route_name").performTextInput(mapName)
        onNodeWithText("Save").performClick()

        waitUntil(timeoutMillis = 40_000L) {
            ensureLoggedIn()
            !hasNodesWithTag("map_saving") &&
                !hasNodesWithText("Saving map...") &&
                hasNodesWithText("Maps") &&
                hasNodesWithText(mapName)
        }
    }

    fun ComposeTestRule.navigateToLibraryTab() {
        onNodeWithText("Library").performClick()
        waitUntil(timeoutMillis = 10_000) {
            hasNodesWithText("Maps")
        }
    }

    private fun ComposeTestRule.isOnMainApp(): Boolean {
        return hasNodesWithText("Map") &&
            hasNodesWithText("Library") &&
            hasNodesWithText("Settings")
    }

    private fun ComposeTestRule.hasNodesWithText(text: String): Boolean {
        return try {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun ComposeTestRule.hasNodesWithTag(tag: String): Boolean {
        return try {
            onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}

class TestEnvironmentRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                InstrumentedTestSupport.prepareTestEnvironment()
                base.evaluate()
            }
        }
    }
}
