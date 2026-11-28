package com.manekdm13.jarvis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisToolRegistryTest {
    @Test fun routineNavigationIsRegisteredAndMayRun() {
        val back = JarvisToolRegistry.admit("back")
        assertTrue(back.allowed)
        assertFalse(back.needsApproval)
        assertEquals("phone.back", back.tool?.id)
        assertTrue(JarvisToolRegistry.mayAutoExecute("open chrome"))
    }

    @Test fun privateDataAndUserInputRequireApproval() {
        assertTrue(JarvisToolRegistry.admit("read screen").needsApproval)
        assertTrue(JarvisToolRegistry.admit("type hello").needsApproval)
        assertFalse(JarvisToolRegistry.mayAutoExecute("tap Send"))
    }

    @Test fun messagesAreDraftOnlyAndRequireApproval() {
        val whatsapp = JarvisToolRegistry.admit("whatsapp Hello there")
        assertEquals("messaging.whatsapp_draft", whatsapp.tool?.id)
        assertTrue(whatsapp.needsApproval)
        assertFalse(JarvisToolRegistry.mayAutoExecute("send whatsapp Hello"))
    }

    @Test fun unknownAndDestructiveCommandsAreNotGrantedTools() {
        val unknown = JarvisToolRegistry.admit("delete all my files")
        assertFalse(unknown.allowed)
        assertTrue(unknown.needsApproval)
        assertNull(unknown.tool)
        assertFalse(JarvisToolRegistry.mayAutoExecute("transfer 5000"))
    }

    @Test fun capabilityCatalogueIsInspectable() {
        assertTrue(JarvisToolRegistry.all().size >= 15)
        assertNotNull(JarvisToolRegistry.find("phone.read_screen"))
        assertNull(JarvisToolRegistry.find("shell.root"))
    }
}
