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

        check(preferences.edit().clear().commit())
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
        assertNotNull("Encrypted profile ciphertext should exist after save", value)
        return requireNotNull(value)
    }

}
