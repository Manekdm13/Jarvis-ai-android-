package com.manekdm13.jarvis

/**
 * Selects the configured inference route. Local-first never silently sends a prompt
 * to the cloud after local failure unless the user explicitly enabled cloud fallback.
 */
internal object JarvisInferenceRouter {
    const val MODE_CLOUD = "cloud"
    const val MODE_LOCAL_FIRST = "local_first"
    const val MODE_LOCAL_ONLY = "local_only"

    data class Result(val text: String, val provider: String, val fallbackUsed: Boolean = false)

    fun mayUseCloudResearch(mode: String, allowCloudFallback: Boolean): Boolean =
        mode.ifBlank { MODE_CLOUD } == MODE_CLOUD || allowCloudFallback

    fun reply(
        mode: String,
        ollamaUrl: String,
        ollamaModel: String,
        cloud: () -> String,
        local: () -> String,
        allowCloudFallback: Boolean = false
    ): Result {
        val selected = mode.ifBlank { MODE_CLOUD }
        return when (selected) {
            MODE_LOCAL_ONLY -> Result(local(), "ollama")
            MODE_LOCAL_FIRST -> try {
                Result(local(), "ollama")
            } catch (localError: Exception) {
                if (!allowCloudFallback) throw IllegalStateException(
                    "Local Ollama failed (${localError.message ?: "unavailable"}). Cloud fallback is disabled; enable it explicitly to send this prompt online."
                )
                Result(cloud(), "cloud", fallbackUsed = true)
            }
            else -> try {
                Result(cloud(), "cloud")
            } catch (cloudError: Exception) {
                if (ollamaUrl.isBlank() || ollamaModel.isBlank()) throw cloudError
                Result(local(), "ollama", fallbackUsed = true)
            }
        }
    }
}
