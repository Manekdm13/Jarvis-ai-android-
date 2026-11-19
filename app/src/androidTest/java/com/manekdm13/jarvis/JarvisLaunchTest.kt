package com.manekdm13.jarvis

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class JarvisLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsAssistantAndApprovalPolicy() {
        composeRule.onNodeWithText("J.A.R.V.I.S").assertExists()
        composeRule.onNodeWithText("Personal Android assistant • You control every phone action")
            .assertExists()
        composeRule.onNodeWithText("Message JARVIS").assertExists()
        composeRule.onNodeWithText("Send").assertExists()
    }
}
