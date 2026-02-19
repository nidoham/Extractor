package com.nidoham.extractor.util

/**
 * Canonical kiosk ID constants for the YouTube service.
 *
 * These IDs must stay in sync with the kioskId values declared in
 * [com.nidoham.extractor.util.KioskTranslator]. If YouTube ever changes an
 * internal kiosk ID, update it here and in KioskTranslator together.
 */
object Kiosk {
    const val TRENDING                       = "Trending"
    const val TRENDING_DISPLAY_NAME          = "Trending"

    const val LIVE                           = "live"
    const val LIVE_DISPLAY_NAME              = "Live"

    const val MUSIC                          = "trending_music"
    const val MUSIC_DISPLAY_NAME             = "Music"

    const val GAMING                         = "trending_gaming"
    const val GAMING_DISPLAY_NAME            = "Gaming"

    const val MOVIES_AND_SHOWS               = "trending_movies_and_shows"
    const val MOVIES_AND_SHOWS_DISPLAY_NAME  = "Movies & Shows"

    const val PODCASTS_EPISODES              = "trending_podcasts_episodes"
    const val PODCASTS_EPISODES_DISPLAY_NAME = "Podcasts & Episodes"
}