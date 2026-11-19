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
        composeRule.onNodeWithText("Personal Android assistant • You control every phone action")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Message JARVIS").assertIsDisplayed()
        composeRule.onNodeWithText("Send").assertIsDisplayed()
    }
}
