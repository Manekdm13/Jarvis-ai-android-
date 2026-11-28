package com.manekdm13.jarvis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisActionProtocolTest {
    @Test fun parsesOnePhoneActionProposal() {
        val parsed = JarvisActionProtocol.parse("""{"type":"phone_action","command":"scroll down","reason":"Move down the current page"}""")
        assertEquals("scroll down", parsed?.command)
        assertEquals("Move down the current page", parsed?.reason)
    }

    @Test fun rejectsNormalTextAndUnknownActionTypes() {
        assertNull(JarvisActionProtocol.parse("Sure, I can help with that."))
        assertNull(JarvisActionProtocol.parse("""{"type":"delete_everything","command":"wipe"}"""))
    }

    @Test fun rejectsEmptyAndOversizedCommands() {
        assertNull(JarvisActionProtocol.parse("""{"type":"phone_action","command":" "}"""))
        assertNull(JarvisActionProtocol.parse("""{"type":"phone_action","command":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}"""))
    }

    @Test fun acceptsFencedJsonButOnlyTheKnownProtocol() {
        val parsed = JarvisActionProtocol.parse(""
            + "```json\n"
            + """{"type":"phone_action","command":"back","reason":"Return to previous screen"}"""
            + "\n```")
        assertEquals("back", parsed?.command)
    }

    @Test fun routineReversibleActionsRunWithoutRepeatedApproval() {
        listOf("back", "home", "scroll down", "open Chrome", "search food safety", "navigate to UAF").forEach {
            assertTrue("$it should be autonomous", JarvisActionPolicy.canAutoExecute(it))
        }
    }

    @Test fun privateOrIrreversibleActionsRemainGated() {
        listOf("tap Send", "type hello", "read screen", "whatsapp hello", "compose sms hello",
            "delete all files", "send email", "make payment", "call 123").forEach {
            assertFalse("$it must require review", JarvisActionPolicy.canAutoExecute(it))
        }
    }

    @Test fun modelCannotDowngradePrivateOrUnknownActions() {
        listOf(
            "read screen", "tap Send", "type my password", "share my documents",
            "send whatsapp my bank details", "delete all files", "transfer 5000",
            "change security settings", "run arbitrary shell command", ""
        ).forEach {
            assertFalse("$it must remain gated", JarvisActionPolicy.canAutoExecute(it))
            assertFalse("$it must not be classified as routine", JarvisActionPolicy.classify(it) == JarvisActionRisk.ROUTINE_REVERSIBLE)
        }
    }

    @Test fun sensitiveSearchQueriesDoNotAutoTransmit() {
        listOf(
            "search my password", "google my API key", "search my bank account",
            "search my medical record", "search my private message"
        ).forEach {
            assertFalse("$it must require review", JarvisActionPolicy.canAutoExecute(it))
            assertEquals(JarvisActionRisk.PRIVATE_DATA_ACCESS, JarvisActionPolicy.classify(it))
        }
    }

    @Test fun riskClassifierSeparatesNavigationFromSensitiveActions() {
        assertEquals(JarvisActionRisk.ROUTINE_REVERSIBLE, JarvisActionPolicy.classify("open Chrome"))
        assertEquals(JarvisActionRisk.EXTERNAL_NAVIGATION, JarvisActionPolicy.classify("search food safety"))
        assertEquals(JarvisActionRisk.PRIVATE_DATA_ACCESS, JarvisActionPolicy.classify("read screen"))
        assertEquals(JarvisActionRisk.USER_INPUT, JarvisActionPolicy.classify("type hello"))
        assertEquals(JarvisActionRisk.EXTERNAL_TRANSMISSION, JarvisActionPolicy.classify("send whatsapp hello"))
        assertEquals(JarvisActionRisk.IRREVERSIBLE_OR_FINANCIAL, JarvisActionPolicy.classify("delete all files"))
        assertEquals(JarvisActionRisk.UNKNOWN, JarvisActionPolicy.classify("run shell"))
    }
}
