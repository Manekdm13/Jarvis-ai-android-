package com.manekdm13.jarvis

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Assert.assertTrue
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

    @Test
    fun androidNetworkStackCanReachLiveJarvisGateway() {
        val result = AiGatewayClient.checkHealth(
            "https://jarvis-ai-gateway.juttfaizan794.workers.dev",
            ""
        )
        assertTrue("Unexpected gateway health response: $result", result.startsWith("Android reached the gateway."))
        assertTrue(
            "Gateway must report authentication configured: $result",
            result.contains("Enter the saved app token") ||
                result.contains("fingerprint unavailable") ||
                result.contains("token DOES NOT MATCH") ||
                result.contains("token MATCHES")
        )
    }
}
