package com.nidoham.extractor.safety.data

/**
 * Result model for content safety checks.
 * @param isBlocked Whether the content should be blocked
 * @param matchedWord The keyword that triggered the block (null if safe)
 * @param category The category of the matched violation (null if safe)
 */
data class SafetyCheckResult(
    val isBlocked: Boolean,
    val matchedWord: String? = null,
    val category: ViolationCategory? = null
)

enum class ViolationCategory {
    ADULT_CONTENT,
    VIOLENCE_HARMFUL,
    HATE_SPEECH,
    DRUGS,
    SPAM_SCAM
}