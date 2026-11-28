package com.manekdm13.jarvis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisInferenceRouterTest {
    @Test fun localOnlyNeverCallsCloud() {
        var cloudCalled = false
        val result = JarvisInferenceRouter.reply(
            mode = JarvisInferenceRouter.MODE_LOCAL_ONLY,
            ollamaUrl = "http://192.168.1.10:11434",
            ollamaModel = "qwen2.5:3b",
            cloud = { cloudCalled = true; "cloud" },
            local = { "local answer" },
            allowCloudFallback = true
        )
        assertEquals("local answer", result.text)
        assertEquals("ollama", result.provider)
        assertFalse(result.fallbackUsed)
        assertFalse(cloudCalled)
    }

    @Test fun localFirstDoesNotSilentlySendToCloud() {
        var cloudCalled = false
        assertThrows(IllegalStateException::class.java) {
            JarvisInferenceRouter.reply(
                mode = JarvisInferenceRouter.MODE_LOCAL_FIRST,
                ollamaUrl = "http://192.168.1.10:11434",
                ollamaModel = "qwen2.5:3b",
                cloud = { cloudCalled = true; "cloud" },
                local = { error("LAN unavailable") },
                allowCloudFallback = false
            )
        }
        assertFalse(cloudCalled)
    }

    @Test fun localFirstUsesCloudOnlyWhenFallbackWasEnabled() {
        var cloudCalled = false
        val result = JarvisInferenceRouter.reply(
            mode = JarvisInferenceRouter.MODE_LOCAL_FIRST,
            ollamaUrl = "http://192.168.1.10:11434",
            ollamaModel = "qwen2.5:3b",
            cloud = { cloudCalled = true; "cloud answer" },
            local = { error("LAN unavailable") },
            allowCloudFallback = true
        )
        assertTrue(cloudCalled)
        assertEquals("cloud", result.provider)
        assertTrue(result.fallbackUsed)
    }

    @Test fun cloudFirstFallsBackToConfiguredLocalModel() {
        val result = JarvisInferenceRouter.reply(
            mode = JarvisInferenceRouter.MODE_CLOUD,
            ollamaUrl = "http://192.168.1.10:11434",
            ollamaModel = "qwen2.5:3b",
            cloud = { error("gateway offline") },
            local = { "local answer" }
        )
        assertEquals("ollama", result.provider)
        assertTrue(result.fallbackUsed)
    }
    @Test fun localOnlyDoesNotPermitCloudResearch() {
        assertFalse(JarvisInferenceRouter.mayUseCloudResearch(JarvisInferenceRouter.MODE_LOCAL_ONLY, false))
        assertFalse(JarvisInferenceRouter.mayUseCloudResearch(JarvisInferenceRouter.MODE_LOCAL_FIRST, false))
        assertTrue(JarvisInferenceRouter.mayUseCloudResearch(JarvisInferenceRouter.MODE_LOCAL_FIRST, true))
        assertTrue(JarvisInferenceRouter.mayUseCloudResearch(JarvisInferenceRouter.MODE_CLOUD, false))
    }

}
