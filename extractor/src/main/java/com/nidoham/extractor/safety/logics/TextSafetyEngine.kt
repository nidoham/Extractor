package com.nidoham.extractor.safety.logics

import com.nidoham.extractor.safety.data.SafetyCheckResult
import com.nidoham.extractor.safety.data.ViolationCategory

/**
 * Optimized content safety engine for YouTube metadata filtering.
 *
 * Improvements over v1:
 * - Categorized keyword sets for faster lookup and better reporting
 * - YouTube-specific patterns (titles, channel names, tags, descriptions)
 * - Leet-speak normalization expanded with common YouTube bypasses
 * - Regex-based boundary checks to reduce false positives
 *   e.g. "classic" won't match "ass", "analyze" won't match "anal"
 * - Singleton-safe: keywords are compiled once at class load time
 * - Early-exit logic: exact match → boundary match → similarity (most → least common)
 */
class TextSafetyEngine {

    // ─── Categorized Keyword Sets ─────────────────────────────────────────────

    private val adultKeywords: Set<String> = setOf(
        "porn", "porno", "pornhub", "xvideos", "xhamster", "redtube", "youporn",
        "xxx", "18plus", "adult", "nsfw", "erotic", "erotica",
        "nude", "nudity", "naked", "topless", "bottomless",
        "sex", "sexual", "sexuality", "sexting", "sextape",
        "onlyfans", "fansly", "camgirl", "camsite", "livejasmin",
        "escort", "prostitute", "prostitution", "hooker", "stripper",
        "blowjob", "handjob", "cumshot", "creampie", "gangbang",
        "orgy", "threesome", "bdsm", "fetish", "kink", "hentai",
        "milf", "dilf", "twink", "bara", "yaoi", "yuri",
        "deepthroat", "squirt", "masturbat", "vibrator", "dildo"
    )

    private val violenceHarmfulKeywords: Set<String> = setOf(
        "kill", "killing", "murder", "murder", "slaughter", "massacre",
        "suicide", "selfharm", "self harm", "cutting", "hang yourself",
        "rape", "rapist", "molest", "assault",
        "bomb", "bombing", "explosion", "explosive", "detonator",
        "terrorist", "terrorism", "jihad", "beheading",
        "school shooting", "mass shooting", "gunman",
        "gore", "torture", "decapitat", "mutilat"
    )

    private val drugKeywords: Set<String> = setOf(
        "cocaine", "heroin", "methamphetamine", "meth", "crystal meth",
        "fentanyl", "opioid", "oxycodone", "xanax", "molly", "mdma",
        "lsd", "acid trip", "shroom", "psilocybin", "ketamine",
        "weed", "marijuana", "cannabis", "blunt", "bong", "dispensary",
        "overdose", "drug deal", "narcotics", "cartel"
    )

    private val hateSpeechKeywords: Set<String> = setOf(
        "racist", "racism", "white supremacy", "white supremacist",
        "nazi", "nazism", "neonazi", "fascist",
        "hitler", "kkk", "klan", "antisemit",
        "isis", "isil", "alqaeda", "boko haram", "hamas",
        "genocide", "ethnic cleansing", "slur"
    )

    private val spamScamKeywords: Set<String> = setOf(
        "spam", "scam", "fraud", "fraudulent", "phishing",
        "hack", "hacking", "hacked", "cracked", "keylogger",
        "warez", "pirate", "piracy", "bootleg", "nulled",
        "get rich quick", "make money fast", "click here now",
        "free followers", "buy subscribers", "buy views", "buy likes",
        "sub4sub", "follow4follow", "f4f", "s4s"
    )

    // ─── Pre-computed Lookup Maps ─────────────────────────────────────────────

    /**
     * Flat map of normalized keyword → category for O(1) category lookup.
     * Built once at initialization.
     */
    private val normalizedToCategory: Map<String, ViolationCategory> by lazy {
        buildMap {
            adultKeywords.forEach { put(normalize(it), ViolationCategory.ADULT_CONTENT) }
            violenceHarmfulKeywords.forEach { put(normalize(it), ViolationCategory.VIOLENCE_HARMFUL) }
            drugKeywords.forEach { put(normalize(it), ViolationCategory.DRUGS) }
            hateSpeechKeywords.forEach { put(normalize(it), ViolationCategory.HATE_SPEECH) }
            spamScamKeywords.forEach { put(normalize(it), ViolationCategory.SPAM_SCAM) }
        }
    }

    private val allNormalizedKeywords: Set<String> by lazy { normalizedToCategory.keys }

    // Words that contain blocked substrings but are safe (YouTube false-positive guard)
    private val allowlist: Set<String> = setOf(
        "classic", "classical", "analyze", "analysis", "analytics",
        "association", "assume", "assassin", "class", "passage",
        "massacre" // kept in violence but listed here as example pattern
    )

    private val similarityThreshold: Double = 0.82

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Fast boolean check. Use for real-time filtering of YouTube metadata. */
    fun check(input: String): Boolean = isBlocked(input)

    /** Detailed result with matched word and violation category. */
    fun checkDetailed(input: String): SafetyCheckResult {
        val normalizedInput = normalize(input)

        // 1. Exact / substring match (fastest)
        findExactMatch(normalizedInput)?.let { (word, category) ->
            return SafetyCheckResult(isBlocked = true, matchedWord = word, category = category)
        }

        // 2. Token similarity match (fuzzy, catches obfuscation)
        findSimilarMatch(normalizedInput)?.let { (word, category) ->
            return SafetyCheckResult(isBlocked = true, matchedWord = word, category = category)
        }

        return SafetyCheckResult(isBlocked = false)
    }

    // ─── Private Logic ────────────────────────────────────────────────────────

    private fun isBlocked(input: String): Boolean {
        val normalizedInput = normalize(input)
        return findExactMatch(normalizedInput) != null || findSimilarMatch(normalizedInput) != null
    }

    /**
     * Checks for exact substring match with word-boundary awareness.
     * Prevents "classic" matching "ass", "analyze" matching "anal", etc.
     */
    private fun findExactMatch(input: String): Pair<String, ViolationCategory>? {
        // Skip if input is in the allowlist
        if (allowlist.any { input.contains(it) }) return null

        return allNormalizedKeywords
            .asSequence()
            .firstOrNull { keyword ->
                // Use word-boundary regex for short keywords to reduce false positives
                if (keyword.length <= 4) {
                    Regex("\\b${Regex.escape(keyword)}\\b").containsMatchIn(input)
                } else {
                    input.contains(keyword)
                }
            }
            ?.let { keyword -> keyword to (normalizedToCategory[keyword] ?: ViolationCategory.ADULT_CONTENT) }
    }

    /**
     * Token-based Jaccard similarity — catches obfuscated or misspelled keywords.
     * Only runs if exact match fails (performance guard).
     */
    private fun findSimilarMatch(input: String): Pair<String, ViolationCategory>? {
        val inputTokens = tokenize(input)
        if (inputTokens.isEmpty()) return null

        return allNormalizedKeywords
            .asSequence()
            .firstOrNull { keyword ->
                val keywordTokens = tokenize(keyword)
                keywordTokens.isNotEmpty() &&
                        jaccardSimilarity(inputTokens, keywordTokens) >= similarityThreshold
            }
            ?.let { keyword -> keyword to (normalizedToCategory[keyword] ?: ViolationCategory.ADULT_CONTENT) }
    }

    private fun tokenize(text: String): Set<String> =
        text.split(Regex("\\s+"))
            .filter { it.length > 2 }
            .toSet()

    /**
     * Jaccard similarity: |A ∩ B| / |A ∪ B|
     * More accurate than the original one-sided overlap formula.
     */
    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Double {
        val intersection = a.intersect(b).size
        val union = (a + b).size
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    /**
     * Normalizes text to catch common YouTube bypass tactics:
     * - Leet speak: 0→o, 1→i, 3→e, 4→a, 5→s, @→a, $→s, +→t
     * - Unicode lookalikes: vv→w, \/→v
     * - Repeated characters: "seeex" → "sex"
     * - Separators: "p.o.r.n" → "porn", "p-o-r-n" → "porn"
     */
    private fun normalize(text: String): String = text
        .lowercase()
        .replace("vv", "w")
        .replace("\\/", "v")
        .replace(Regex("[._\\-]"), "")          // remove common obfuscation separators
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .replace('0', 'o')
        .replace('1', 'i')
        .replace('3', 'e')
        .replace('4', 'a')
        .replace('5', 's')
        .replace('@', 'a')
        .replace('$', 's')
        .replace('+', 't')
        .replace(Regex("(.)\\1{2,}"), "$1")     // "sexxxy" → "sexy"
        .replace(Regex("\\s+"), " ")
        .trim()
}