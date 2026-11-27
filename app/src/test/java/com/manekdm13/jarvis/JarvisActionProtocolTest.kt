package com.manekdm13.jarvis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarvisActionProtocolTest {
    @Test
    fun parsesOnePhoneActionProposal() {
        val parsed = JarvisActionProtocol.parse(
            """{"type":"phone_action","command":"scroll down","reason":"Move down the current page"}"""
        )
        assertEquals("scroll down", parsed?.command)
        assertEquals("Move down the current page", parsed?.reason)
    }

    @Test
    fun rejectsNormalTextAndUnknownActionTypes() {
        assertNull(JarvisActionProtocol.parse("Sure, I can help with that."))
        assertNull(JarvisActionProtocol.parse("""{"type":"delete_everything","command":"wipe"}"""))
    }

    @Test
    fun rejectsEmptyAndOversizedCommands() {
        assertNull(JarvisActionProtocol.parse("""{"type":"phone_action","command":" "}"""))
        assertNull(
            JarvisActionProtocol.parse(
                """{"type":"phone_action","command":"${"x".repeat(2001)}"}"""
            )
        )
    }

    @Test
    fun acceptsFencedJsonButOnlyTheKnownProtocol() {
        val parsed = JarvisActionProtocol.parse(
            "```json\n" +
                """{"type":"phone_action","command":"back","reason":"Return to previous screen"}""" +
                "\n```"
        )
        assertEquals("back", parsed?.command)
    }
}
