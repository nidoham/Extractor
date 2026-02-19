package com.nidoham.extractor.util

import com.nidoham.extractor.stream.StreamItem

/**
 * Single source of truth for all kiosk categories.
 * Display name (shown in chip) is decoupled from the actual NewPipe kiosk ID.
 * expectedType tells ContentScreen what kind of StreamItem to expect.
 */
enum class KioskTranslator(
    val displayName: String,
    val kioskId: String,
    val expectedType: StreamItem.ItemType = StreamItem.ItemType.VIDEO
) {
    TRENDING(
        displayName = "Trending",
        kioskId = "Trending"
    ),

    LIVE(
        displayName = "Live",
        kioskId = "live"
    ),

    MUSIC(
        displayName = "Music",
        kioskId = "trending_music"
    ),

    SPORTS(
        displayName = "Gaming",
        kioskId = "trending_gaming"
    ),

    MOVIES(
        displayName = "Movies & Shows",
        kioskId = "trending_movies_and_shows"
    ),

    TECH(
        displayName = "Podcasts & Episodes",
        kioskId = "trending_podcasts_episodes",
    );

    companion object {
        /**
         * Resolve a kiosk entry from a raw kiosk ID string.
         * @param id The kiosk ID to search for
         * @param fallback The fallback kiosk if not found (default: TRENDING)
         * @return Matched KioskTranslator or fallback
         */
        fun fromKioskId(id: String, fallback: KioskTranslator = TRENDING): KioskTranslator =
            entries.find { it.kioskId.equals(id, ignoreCase = true) } ?: fallback

        /**
         * Resolve a kiosk entry from display name.
         * @param name The display name to search for
         * @return Matched KioskTranslator or null
         */
        fun fromDisplayName(name: String): KioskTranslator? =
            entries.find { it.displayName.equals(name, ignoreCase = true) }

        /**
         * All display names in order — used to populate chips.
         */
        val displayNames: List<String>
            get() = entries.map { it.displayName }

        /**
         * All kiosk IDs in order — useful for debugging/logging.
         */
        val kioskIds: List<String>
            get() = entries.map { it.kioskId }

        /**
         * Get kiosks filtered by expected type.
         * @param type The StreamItem type to filter by
         * @return List of matching kiosks
         */
        fun getByType(type: StreamItem.ItemType): List<KioskTranslator> =
            entries.filter { it.expectedType == type }

        /**
         * Default kiosk for initial app state.
         */
        val DEFAULT: KioskTranslator = TRENDING
    }

    /**
     * Check if this kiosk matches the given ID.
     */
    fun matches(id: String): Boolean =
        this.kioskId.equals(id, ignoreCase = true)

    /**
     * Check if this kiosk is a video-based kiosk.
     */
    val isVideoKiosk: Boolean
        get() = expectedType == StreamItem.ItemType.VIDEO
}