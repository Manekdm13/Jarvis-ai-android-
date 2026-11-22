package com.manekdm13.jarvis

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class JarvisLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsAssistantAndApprovalPolicy() {
        composeRule.onNodeWithText("J.A.R.V.I.S").assertIsDisplayed()

        // Match stable visible labels from the current dashboard and composer.
        composeRule.onNodeWithText("Your assistant is ready").assertIsDisplayed()
        composeRule.onNodeWithText("Message your assistant…").assertIsDisplayed()
        composeRule.onNodeWithText("Send  ➤").assertIsDisplayed()
    }
}
