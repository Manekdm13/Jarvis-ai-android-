package com.manekdm13.jarvis

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional user-enabled bridge for interacting with visible Android UI.
 * Commands are dispatched only after the JARVIS in-app one-time approval.
 * This service does not bypass app sandboxes or expose hidden/private app databases.
 */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        active = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally do not collect or persist screen contents in the background.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (active === this) active = null
        super.onDestroy()
    }

    fun executeApprovedCommand(command: String): Boolean {
        val root = rootInActiveWindow
        return when {
            command == "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            command == "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            command == "scroll_down" -> scroll(root, forward = true)
            command == "scroll_up" -> scroll(root, forward = false)
            command.startsWith("tap:") -> tapVisibleText(root, command.removePrefix("tap:"))
            command.startsWith("type:") -> typeIntoFocusedField(command.removePrefix("type:"))
            else -> false
        }
    }

    fun readVisibleScreen(): String {
        val root = rootInActiveWindow ?: return ""
        val collected = linkedSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && collected.sumOf { it.length } < 7_000) {
            val node = queue.removeFirst()
            if (!node.isPassword) {
                node.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(collected::add)
                node.contentDescription?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(collected::add)
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return collected.joinToString("\\n").take(7_000)
    }

    private fun tapVisibleText(root: AccessibilityNodeInfo?, target: String): Boolean {
        if (root == null || target.isBlank()) return false
        val matches = root.findAccessibilityNodeInfosByText(target)
        for (match in matches) {
            var candidate: AccessibilityNodeInfo? = match
            while (candidate != null) {
                if (candidate.isClickable && candidate.isEnabled) {
                    return candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                candidate = candidate.parent
            }
        }
        return false
    }

    private fun typeIntoFocusedField(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        if (!focused.isEditable || focused.isPassword) return false
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                value.take(2000)
            )
        }
        return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun scroll(root: AccessibilityNodeInfo?, forward: Boolean): Boolean {
        if (root == null) return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable && node.isEnabled) {
                val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                    else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                if (node.performAction(action)) return true
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(queue::addLast)
        }
        return false
    }

    companion object {
        @Volatile private var active: JarvisAccessibilityService? = null

        fun isRunning(): Boolean = active != null

        fun execute(command: String): Boolean =
            active?.executeApprovedCommand(command) ?: false

        fun readScreen(): String = active?.readVisibleScreen().orEmpty()

    }
}
