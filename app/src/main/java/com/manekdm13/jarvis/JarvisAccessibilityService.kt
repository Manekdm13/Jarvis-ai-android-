package com.manekdm13.jarvis

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/** User-enabled bridge for explicit, user-directed Android UI assistance. */
class JarvisAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() { super.onServiceConnected(); instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { if (instance === this) instance = null; super.onDestroy() }

    fun readVisibleScreen(): String {
        val root = rootInActiveWindow ?: return "No accessible screen is currently available."
        val lines = ArrayList<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && lines.size < 160) {
            val node = queue.removeFirst()
            val text = node.text?.toString()?.trim().orEmpty()
            val desc = node.contentDescription?.toString()?.trim().orEmpty()
            val value = when {
                text.isNotBlank() && desc.isNotBlank() && !text.equals(desc, true) -> "$text ($desc)"
                text.isNotBlank() -> text
                else -> desc
            }
            if (value.isNotBlank() && lines.lastOrNull() != value) lines.add(value)
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return lines.distinct().take(100).joinToString("\n").ifBlank { "No readable text is exposed on this screen." }
    }

    fun clickText(label: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val target = queue.removeFirst()
            val text = target.text?.toString().orEmpty()
            val desc = target.contentDescription?.toString().orEmpty()
            if (text.contains(label, true) || desc.contains(label, true)) {
                var node: AccessibilityNodeInfo? = target
                while (node != null) {
                    if (node.isClickable) return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    node = node.parent
                }
                return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            for (i in 0 until target.childCount) target.getChild(i)?.let(queue::addLast)
        }
        return false
    }

    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable) return node.performAction(
                if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            )
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return false
    }

    companion object {
        @Volatile private var instance: JarvisAccessibilityService? = null
        fun isRunning(): Boolean = instance != null
        fun readScreen(): String = instance?.readVisibleScreen()
            ?: "Accessibility access is not enabled. Enable JARVIS in Android Settings → Accessibility."
        fun tapText(label: String): Boolean = instance?.clickText(label) ?: false
        fun scroll(forward: Boolean): Boolean = instance?.scroll(forward) ?: false
        fun globalBack(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        fun globalHome(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
    }
}
