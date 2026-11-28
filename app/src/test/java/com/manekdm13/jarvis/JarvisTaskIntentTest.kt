package com.manekdm13.jarvis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisTaskIntentTest {
    @Test fun recognizesObjectiveRequestsForAutonomousExecution() {
        listOf(
            "Research fully funded food science scholarships in Europe",
            "Find me legitimate online income opportunities in Pakistan",
            "Compare these universities and recommend the best option",
            "Please prepare my application checklist",
            "Can you solve this spreadsheet problem",
            "I want you to organize my research notes",
            "Take care of finding suitable internships",
            "I need you to find paid internships in Pakistan"
        ).forEach { assertTrue(it, JarvisTaskIntent.isAutonomousTaskRequest(it)) }
    }

    @Test fun leavesQuestionsAndDirectPhoneCommandsInNormalChatPath() {
        listOf(
            "What is HACCP?",
            "How do I research a topic?",
            "What time is it?",
            "open Chrome",
            "scroll down",
            "hello JARVIS"
        ).forEach { assertFalse(it, JarvisTaskIntent.isAutonomousTaskRequest(it)) }
    }
}
