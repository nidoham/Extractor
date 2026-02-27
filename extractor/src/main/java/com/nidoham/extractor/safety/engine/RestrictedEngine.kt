package com.nidoham.extractor.safety.engine

import com.nidoham.extractor.safety.data.SafetyCheckResult
import com.nidoham.extractor.safety.logics.TextSafetyEngine

/**
 * Singleton wrapper around [TextSafetyEngine].
 *
 * The engine is initialized once (lazy) and reused across all calls —
 * avoids re-building the keyword maps on every YouTube metadata check.
 *
 * Usage:
 *   RestrictedEngine.check("video title here")          // → Boolean
 *   RestrictedEngine.checkDetailed("video title here")  // → SafetyCheckResult
 */
object RestrictedEngine {

    private val engine: TextSafetyEngine by lazy { TextSafetyEngine() }

    /**
     * Returns true if the content should be blocked.
     * Suitable for fast, high-volume YouTube metadata checks.
     */
    fun check(text: String): Boolean = engine.check(text)

    /**
     * Returns a [SafetyCheckResult] with the matched word and violation category.
     * Use this when you need to log or display the reason for blocking.
     */
    fun checkDetailed(text: String): SafetyCheckResult = engine.checkDetailed(text)

    /**
     * Checks a YouTube video's full metadata bundle in one call.
     * Combines title + description + tags into a single safety check.
     */
    fun checkYouTubeMetadata(
        title: String,
        description: String = "",
        tags: List<String> = emptyList()
    ): SafetyCheckResult {
        val combined = buildString {
            append(title)
            if (description.isNotBlank()) append(" ").append(description.take(500))
            if (tags.isNotEmpty()) append(" ").append(tags.joinToString(" "))
        }
        return engine.checkDetailed(combined)
    }
}