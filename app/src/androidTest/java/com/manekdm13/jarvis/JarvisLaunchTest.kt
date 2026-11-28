package com.manekdm13.jarvis

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class JarvisLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsAssistantAndApprovalPolicy() {
        composeRule.onNodeWithText("J.A.R.V.I.S").assertIsDisplayed()
        composeRule.onNodeWithText("One assistant for conversation, files, research and phone actions.").assertIsDisplayed()
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

    @Test
    fun conversationMemoryPersistsEncryptedMessagesAndRejectsCorruptData() {
        // Use instrumentation-private preferences; never touch the installed app's
        // actual conversation history.
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val preferences = testContext.getSharedPreferences(
            "jarvis_private_memory",
            Context.MODE_PRIVATE
        )
        preferences.edit().clear().apply()

        val privateMessage = "JARVIS conversation encryption regression test"
        ConversationMemory(testContext).save(listOf(privateMessage))

        val storedCiphertext = assertNotNullAndGet(
            preferences.getString("conversation_ciphertext", null)
        )
        assertFalse(
            "Conversation plaintext must not be stored in preferences",
            storedCiphertext.contains(privateMessage)
        )
        assertEquals(
            "A fresh memory instance should restore the saved conversation",
            listOf(privateMessage),
            ConversationMemory(testContext).load()
        )

        // A malformed packed IV/ciphertext must be discarded safely, not crash
        // startup or return corrupted conversation text.
        preferences.edit().putString("conversation_ciphertext", "AQID").apply()
        assertEquals(emptyList<String>(), ConversationMemory(testContext).load())
        assertFalse(
            "Corrupt ciphertext should be removed after the failed decrypt",
            preferences.contains("conversation_ciphertext")
        )
    }

    @Test
    fun personalProfileIsOptInEncryptedAndClearable() {
        // Use the instrumentation package's private preferences so this test never
        // reads, overwrites, or clears the owner's real JARVIS profile.
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val profileMemory = UserProfileMemory(testContext)
        val preferences = testContext.getSharedPreferences(
            "jarvis_private_profile",
            Context.MODE_PRIVATE
        )

        // apply() updates the in-memory preferences synchronously. Do not assert
        // the asynchronous disk-write result: some emulator images report false
        // here even though the isolated preferences are correctly cleared.
        preferences.edit().clear().apply()
        val suggestedProfile = profileMemory.load()
        assertTrue("The editable suggestion should be available", suggestedProfile.isNotBlank())
        assertEquals("An unsaved suggestion must not be sent to online AI", "", profileMemory.loadSaved())

        val privateText = "JARVIS profile encryption test"
        profileMemory.save(privateText)
        assertEquals(privateText, profileMemory.loadSaved())

        val storedCiphertext = assertNotNullAndGet(
            preferences.getString("profile_ciphertext", null)
        )
        assertFalse("Profile plaintext must not be stored in preferences", storedCiphertext.contains(privateText))

        profileMemory.clear()
        assertEquals("Clearing must disable profile context", "", profileMemory.loadSaved())
    }

    private fun assertNotNullAndGet(value: String?): String {
        assertNotNull("Encrypted ciphertext should exist after save", value)
        return requireNotNull(value)
    }
}
