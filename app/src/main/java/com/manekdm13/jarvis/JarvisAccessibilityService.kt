package com.manekdm13.jarvis

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Opt-in accessibility foundation for future user-directed phone actions.
 *
 * This first milestone intentionally observes only lifecycle connection. It does
 * not read screen text, click controls, type, or transmit accessibility data.
 * Any future interaction must be initiated by the user and separately approved.
 */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Deliberately no screen-content collection or automatic actions in this milestone.
    }

    override fun onInterrupt() = Unit
}
