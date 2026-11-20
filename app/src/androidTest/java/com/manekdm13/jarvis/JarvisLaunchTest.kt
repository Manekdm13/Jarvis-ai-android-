package com.manekdm13.jarvis

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test
    fun phoneActionWaitsForApprovalAndCanBeDenied() {
        composeRule.onNodeWithText("Message JARVIS").performTextInput("open settings")
        composeRule.onNodeWithText("Send").performClick()

        composeRule.onNodeWithText("JARVIS needs your approval").assertIsDisplayed()
        composeRule.onNodeWithText("Approve once").assertIsDisplayed()
        composeRule.onNodeWithText("Deny").performClick()
        composeRule.onNodeWithText("Denied. I cancelled that action and did not run it.")
            .assertIsDisplayed()
    }

    @Test
    fun conversationMemoryRoundTripsEncryptedCiphertext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences(
            "jarvis_private_memory",
            Context.MODE_PRIVATE
        )
        preferences.edit().clear().commit()

        val marker = "private-memory-test-${System.nanoTime()}"
        try {
            ConversationMemory(context).save(listOf(marker))

            val storedCiphertext = preferences.getString("conversation_ciphertext", null)
            assertNotNull(storedCiphertext)
            assertFalse(requireNotNull(storedCiphertext).contains(marker))
            assertEquals(listOf(marker), ConversationMemory(context).load())
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
