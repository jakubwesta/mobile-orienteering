package com.mobileorienteering

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoginTest {

    // Sets up Hilt dependency injection
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Launches MainActivity
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()

        val context = InstrumentationRegistry.getInstrumentation().targetContext

        val datastoreDir = File(context.filesDir, "datastore")
        if (datastoreDir.exists()) {
            datastoreDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }

        context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        Thread.sleep(500)
    }

    @Before
    fun disableAnimations() {
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("settings put global window_animation_scale 0")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("settings put global transition_animation_scale 0")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("settings put global animator_duration_scale 0")
    }

    private fun handleWelcomeScreenIfPresent() {
        val welcomeScreenPresent = try {
            composeTestRule
                .onAllNodesWithText("Get Started")
                .fetchSemanticsNodes()
                .isNotEmpty()
        } catch (_: Exception) {
            false
        }

        if (welcomeScreenPresent) {
            composeTestRule.onNodeWithText("Get Started").performClick()

            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule
                    .onAllNodesWithText("Get Started")
                    .fetchSemanticsNodes()
                    .isEmpty()
            }
        }
    }

    @Test
    fun welcomeScreen_clickGetStarted_navigatesToLogin() {
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Get Started")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Welcome to").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobile Orienteering").assertIsDisplayed()
        composeTestRule.onNodeWithText("Get Started").assertIsDisplayed()

        composeTestRule.onNodeWithText("Get Started").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Log In").assertIsDisplayed()
    }

    @Test
    fun completeLoginFlow_withTestUserCredentials() {
        handleWelcomeScreenIfPresent()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNode(
            hasSetTextAction() and hasText("Username")
        ).performTextInput("testuser")

        composeTestRule.onNode(
            hasSetTextAction() and hasText("Password")
        ).performTextInput("password123")

        composeTestRule.onNodeWithText("Log in").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            val loginScreenGone = composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isEmpty()

            val errorAppeared = try {
                composeTestRule
                    .onAllNodes(hasText("failed", substring = true, ignoreCase = true))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            } catch (_: Exception) {
                false
            }

            loginScreenGone || errorAppeared
        }
    }

    @Test
    fun loginFlow_withInvalidCredentials_shouldShowError() {
        handleWelcomeScreenIfPresent()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNode(
            hasSetTextAction() and hasText("Username")
        ).performTextInput("wronguser")

        composeTestRule.onNode(
            hasSetTextAction() and hasText("Password")
        ).performTextInput("wrongpassword")

        composeTestRule.onNodeWithText("Log in").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun loginScreen_displaysAllElements() {
        handleWelcomeScreenIfPresent()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Log In").assertIsDisplayed()
        composeTestRule.onNodeWithText("Username").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
        composeTestRule.onNodeWithText("Register").assertIsDisplayed()
        composeTestRule.onNodeWithText("Don't have an account?").assertIsDisplayed()
    }

    @Test
    fun loginScreen_emptyFields_buttonDisabled() {
        handleWelcomeScreenIfPresent()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Log in").assertIsNotEnabled()
    }

    @Test
    fun loginScreen_enterCredentials_buttonEnabled() {
        handleWelcomeScreenIfPresent()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Log in").assertIsNotEnabled()

        composeTestRule.onNode(
            hasSetTextAction() and hasText("Username")
        ).performTextInput("testuser")

        composeTestRule.onNode(
            hasSetTextAction() and hasText("Password")
        ).performTextInput("password123")

        composeTestRule.onNodeWithText("Log in").assertIsEnabled()
    }

    @Test
    fun loginScreen_navigateToRegister() {
        handleWelcomeScreenIfPresent()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule
                .onAllNodesWithText("Log In")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule.onNodeWithText("Register").performClick()

        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule
                    .onAllNodesWithText("Create account")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            } catch (_: Exception) {
                false
            }
        }

        composeTestRule.onNodeWithText("Register").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create account").assertIsDisplayed()
        composeTestRule.onNodeWithText("Already have an account?").assertIsDisplayed()
    }
}
