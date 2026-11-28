package com.manekdm13.jarvis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarvisAutoLearningTest {
    @Test
    fun learnsExplicitLongTermPreference() {
        assertEquals(
            "User preference: answer me concisely",
            JarvisAutoLearning.extractExplicitLesson("From now on, answer me concisely.")
        )
    }

    @Test
    fun learnsLanguageAndInteractionPreferences() {
        assertEquals(
            "User preference: speak Urdu when I use Urdu",
            JarvisAutoLearning.extractExplicitLesson("I want JARVIS to speak Urdu when I use Urdu")
        )
    }

    @Test
    fun ignoresOrdinaryConversation() {
        assertNull(JarvisAutoLearning.extractExplicitLesson("I had a long day today."))
    }

    @Test
    fun refusesCredentialLikeContent() {
        assertNull(JarvisAutoLearning.extractExplicitLesson("From now on remember my API key is abc123"))
        assertNull(JarvisAutoLearning.extractExplicitLesson("Always use recovery code 12345678"))
    }

    @Test
    fun rejectsShortOrVaguePreferences() {
        assertNull(JarvisAutoLearning.extractExplicitLesson("Always do it"))
        assertNull(JarvisAutoLearning.extractExplicitLesson("I prefer"))
    }
}
