package com.nidoham.extractor.util

/**
 * Centralized repository of SharedPreferences keys and default values for the application.
 *
 * This object serves as the single source of truth for all preference keys used throughout the app,
 * promoting consistency, preventing typos, and making preference management more maintainable.
 * Keys are organized into logical categories for easy navigation and discovery.
 *
 * ## Design Philosophy
 * - **Type Safety**: Using object constants prevents string literal typos
 * - **Discoverability**: IDE autocomplete helps find available preferences
 * - **Maintainability**: Centralized location makes refactoring easier
 * - **Documentation**: Each category and key is documented for clarity
 * - **Default Values**: The Defaults nested object provides recommended initial values
 *
 * ## Organization
 * Preferences are organized into the following categories:
 * - **Debug & Logging**: Error logs, debug modes, performance monitoring
 * - **Appearance & Theme**: Visual styling, colors, layouts, typography
 * - **Content & Localization**: Language, region, content filtering
 * - **Playback**: Video quality, player behavior, audio settings, subtitles
 * - **Downloads**: Download location, quality, network preferences
 * - **Network & Data**: Data saving, caching, image quality
 * - **Navigation & Behavior**: UI navigation, gestures, history
 * - **Notifications**: Notification preferences and behaviors
 * - **TV Specific**: Android TV and lean-back mode settings
 * - **Privacy & Security**: Incognito mode, history clearing, authentication
 * - **Advanced**: Experimental features, performance tuning
 * - **First Run & Onboarding**: Setup and initialization tracking
 *
 * ## Usage Examples
 * ```kotlin
 * // Reading preferences
 * val theme = PreferenceHelper.getString(Preferences.THEME, Preferences.Defaults.THEME)
 * val autoPlay = settings.getBoolean(Preferences.AUTO_PLAY, Preferences.Defaults.AUTO_PLAY)
 *
 * // Writing preferences
 * PreferenceHelper.putString(Preferences.THEME, "dark")
 * settings.edit { putBoolean(Preferences.AUTO_PLAY, false) }
 *
 * // Using in XML preferences
 * <SwitchPreference
 *     android:key="@string/pref_auto_play"
 *     android:title="Auto-play videos"
 *     android:defaultValue="true" />
 * ```
 *
 * ## Platform Compatibility
 * All preferences are compatible with:
 * - Android 8.0+ (API 26+)
 * - Phone and tablet form factors
 * - Android TV (includes TV-specific preferences)
 *
 * ## Migration Notes
 * - Deprecated preferences are marked with `@Deprecated` annotation
 * - Migration logic should be added when removing or renaming keys
 * - Use [Defaults] object for safe default values
 *
 * @see PreferenceHelper for reading and writing preferences
 * @see Defaults for recommended default values
 */
object Preferences {

    /* ========================================================================
       DEBUG & LOGGING
       ======================================================================== */

    /**
     * Key for storing error log messages for debugging and crash reporting.
     *
     * Type: String
     * Default: "" (empty string)
     */
    const val ERROR_LOG = "error_log"

    /**
     * Key for enabling debug mode throughout the application.
     *
     * Type: Boolean
     * Default: false
     */
    const val DEBUG_MODE = "debug_mode"

    /**
     * Key for showing performance statistics overlay in the UI.
     *
     * Type: Boolean
     * Default: false
     */
    const val SHOW_PERFORMANCE_STATS = "show_performance_stats"

    /* ========================================================================
       APPEARANCE & THEME
       ======================================================================== */

    /**
     * Key for theme mode selection (system/light/dark/amoled).
     *
     * Type: String
     * Values: "system", "light", "dark", "amoled"
     * Default: "system"
     */
    const val THEME = "pref_theme"

    /**
     * Key for enabling dynamic color (Material You theming on Android 12+).
     *
     * Type: Boolean
     * Default: false
     * Requires: Android 12+ (API 31+)
     */
    const val DYNAMIC_COLOR = "pref_dynamic_color"

    /**
     * Key for AMOLED mode (pure black backgrounds for OLED displays).
     *
     * Type: Boolean
     * Default: false
     *
     * @deprecated Use [THEME] with "amoled" value instead for better theme management.
     */
    @Deprecated("Use THEME with 'amoled' value instead")
    const val AMOLED_MODE = "pref_amoled_mode"

    /**
     * Key for number of grid columns in portrait orientation.
     *
     * Type: Int
     * Range: 1-4
     * Default: 2
     */
    const val GRID_COLUMNS_PORTRAIT = "pref_grid_columns_portrait"

    /**
     * Key for number of grid columns in landscape orientation.
     *
     * Type: Int
     * Range: 2-6
     * Default: 3
     */
    const val GRID_COLUMNS_LANDSCAPE = "pref_grid_columns_landscape"

    /**
     * Key for number of grid columns on Android TV.
     *
     * Type: Int
     * Range: 3-8
     * Default: 4
     */
    const val GRID_COLUMNS_TV = "pref_grid_columns_tv"

    /**
     * Key for UI scale multiplier.
     *
     * Type: Float
     * Range: 0.8 - 1.2
     * Default: 1.0
     */
    const val UI_SCALE = "pref_ui_scale"

    /**
     * Key for compact mode (reduced spacing and padding).
     *
     * Type: Boolean
     * Default: false
     */
    const val COMPACT_MODE = "pref_compact_mode"

    /**
     * Key for font size multiplier.
     *
     * Type: Float
     * Range: 0.8 - 1.5
     * Default: 1.0
     */
    const val FONT_SIZE_MULTIPLIER = "pref_font_size_multiplier"

    /**
     * Key for using system font instead of custom app font.
     *
     * Type: Boolean
     * Default: false
     */
    const val USE_SYSTEM_FONT = "pref_use_system_font"

    /**
     * Key for TV UI mode selection.
     *
     * Type: String
     * Values: "lean_back", "standard"
     * Default: "lean_back"
     */
    const val TV_UI_MODE = "pref_tv_ui_mode"

    /**
     * Key for TV background opacity level.
     *
     * Type: Float
     * Range: 0.0 - 1.0
     * Default: 0.7
     */
    const val TV_BACKGROUND_OPACITY = "pref_tv_background_opacity"

    /**
     * Key for enabling focus animations on TV.
     *
     * Type: Boolean
     * Default: true
     */
    const val TV_FOCUS_ANIMATION = "pref_tv_focus_animation"

    /* ========================================================================
       CONTENT & LOCALIZATION
       ======================================================================== */

    /**
     * Key for app language (UI language).
     *
     * Type: String
     * Values: "sys" (system default) or ISO 639-1 codes ("en", "es", "zh", etc.)
     * Default: "sys"
     */
    const val APP_LANGUAGE = "pref_app_language"

    /**
     * Key for content language (language of fetched content).
     *
     * Type: String
     * Values: "sys" (system default) or ISO 639-1 codes
     * Default: "sys"
     */
    const val CONTENT_LANGUAGE = "pref_content_language"

    /**
     * Key for region/country for trending content.
     *
     * Type: String
     * Values: "sys" (auto-detect) or ISO 3166-1 alpha-2 codes ("US", "GB", "JP", etc.)
     * Default: "sys"
     */
    const val REGION = "pref_region"

    /**
     * Key for showing age-restricted content.
     *
     * Type: Boolean
     * Default: false
     */
    const val SHOW_AGE_RESTRICTED = "pref_show_age_restricted"

    /**
     * Key for content country filtering.
     *
     * Type: String
     * Values: ISO 3166-1 alpha-2 country codes
     * Default: "" (no filtering)
     */
    const val CONTENT_COUNTRY = "pref_content_country"

    /* ========================================================================
       PLAYBACK
       ======================================================================== */

    /**
     * Key for default video resolution.
     *
     * Type: String
     * Values: "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p", "auto"
     * Default: "720p"
     */
    const val DEFAULT_RESOLUTION = "pref_default_resolution"

    /**
     * Key for default video resolution on Wi-Fi networks.
     *
     * Type: String
     * Values: Same as DEFAULT_RESOLUTION
     * Default: "1080p"
     */
    const val DEFAULT_RESOLUTION_WIFI = "pref_default_resolution_wifi"

    /**
     * Key for default video resolution on mobile data.
     *
     * Type: String
     * Values: Same as DEFAULT_RESOLUTION
     * Default: "720p"
     */
    const val DEFAULT_RESOLUTION_MOBILE = "pref_default_resolution_mobile"

    /**
     * Key for default video resolution on Android TV.
     *
     * Type: String
     * Values: Same as DEFAULT_RESOLUTION
     * Default: "1080p"
     */
    const val DEFAULT_RESOLUTION_TV = "pref_default_resolution_tv"

    /**
     * Key for auto-playing videos when opened.
     *
     * Type: Boolean
     * Default: true
     */
    const val AUTO_PLAY = "pref_auto_play"

    /**
     * Key for auto-playing next video in playlist/queue.
     *
     * Type: Boolean
     * Default: false
     */
    const val AUTO_PLAY_NEXT = "pref_auto_play_next"

    /**
     * Key for enabling background playback (audio continues when app is minimized).
     *
     * Type: Boolean
     * Default: false
     */
    const val BACKGROUND_PLAYBACK = "pref_background_playback"

    /**
     * Key for enabling picture-in-picture mode.
     *
     * Type: Boolean
     * Default: true
     * Requires: Android 8.0+ (API 26+)
     */
    const val PICTURE_IN_PICTURE = "pref_picture_in_picture"

    /**
     * Key for remembering and resuming playback position.
     *
     * Type: Boolean
     * Default: true
     */
    const val REMEMBER_PLAYBACK_POSITION = "pref_remember_position"

    /**
     * Key for default audio language preference.
     *
     * Type: String
     * Values: ISO 639-1 language codes
     * Default: "sys"
     */
    const val DEFAULT_AUDIO_LANGUAGE = "pref_default_audio_language"

    /**
     * Key for preferred audio format.
     *
     * Type: String
     * Values: "m4a", "webm", "opus"
     * Default: "m4a"
     */
    const val DEFAULT_AUDIO_FORMAT = "pref_default_audio_format"

    /**
     * Key for enabling volume gesture control.
     *
     * Type: Boolean
     * Default: true
     */
    const val VOLUME_GESTURE = "pref_volume_gesture"

    /**
     * Key for enabling brightness gesture control.
     *
     * Type: Boolean
     * Default: true
     */
    const val BRIGHTNESS_GESTURE = "pref_brightness_gesture"

    /**
     * Key for showing captions/subtitles by default.
     *
     * Type: Boolean
     * Default: false
     */
    const val SHOW_CAPTIONS = "pref_show_captions"

    /**
     * Key for preferred caption language.
     *
     * Type: String
     * Values: ISO 639-1 language codes or "auto"
     * Default: "auto"
     */
    const val CAPTION_LANGUAGE = "pref_caption_language"

    /**
     * Key for caption text size.
     *
     * Type: String
     * Values: "small", "medium", "large"
     * Default: "medium"
     */
    const val CAPTION_SIZE = "pref_caption_size"

    /**
     * Key for caption style/appearance.
     *
     * Type: String
     * Values: "default", "yellow_black", "white_black", "black_white"
     * Default: "default"
     */
    const val CAPTION_STYLE = "pref_caption_style"

    /* ========================================================================
       DOWNLOADS
       ======================================================================== */

    /**
     * Key for download directory path.
     *
     * Type: String
     * Default: "" (uses app's default downloads folder)
     */
    const val DOWNLOAD_PATH = "pref_download_path"

    /**
     * Key for default download quality.
     *
     * Type: String
     * Values: "144p", "240p", "360p", "480p", "720p", "1080p", "1440p", "2160p", "best"
     * Default: "720p"
     */
    const val DOWNLOAD_QUALITY = "pref_download_quality"

    /**
     * Key for restricting downloads to Wi-Fi only.
     *
     * Type: Boolean
     * Default: true
     */
    const val DOWNLOAD_OVER_WIFI_ONLY = "pref_download_wifi_only"

    /**
     * Key for maximum number of simultaneous downloads.
     *
     * Type: Int
     * Range: 1-5
     * Default: 3
     */
    const val MAX_SIMULTANEOUS_DOWNLOADS = "pref_max_downloads"

    /* ========================================================================
       NETWORK & DATA
       ======================================================================== */

    /**
     * Key for data saving mode (reduces quality and caching).
     *
     * Type: Boolean
     * Default: false
     */
    const val DATA_SAVING_MODE = "pref_data_saving_mode"

    /**
     * Key for preloading thumbnails before needed.
     *
     * Type: Boolean
     * Default: true
     */
    const val PRELOAD_THUMBNAILS = "pref_preload_thumbnails"

    /**
     * Key for image quality preference.
     *
     * Type: String
     * Values: "high", "medium", "low"
     * Default: "high"
     */
    const val IMAGE_QUALITY = "pref_image_quality"

    /**
     * Key for using external video player apps.
     *
     * Type: Boolean
     * Default: false
     */
    const val USE_EXTERNAL_PLAYER = "pref_use_external_player"

    /**
     * Key for cache size limit in megabytes.
     *
     * Type: Int
     * Range: 50-1000 MB
     * Default: 200
     */
    const val CACHE_SIZE = "pref_cache_size"

    /**
     * Key for metadata cache expiry time in seconds.
     *
     * Type: Int
     * Default: 3600 (1 hour)
     */
    const val METADATA_CACHE_EXPIRY = "pref_metadata_cache_expiry"

    /**
     * Key for clearing cache when exiting the app.
     *
     * Type: Boolean
     * Default: false
     */
    const val CLEAR_CACHE_ON_EXIT = "pref_clear_cache_on_exit"

    /* ========================================================================
       NAVIGATION & BEHAVIOR
       ======================================================================== */

    /**
     * Key for the screen to show on app launch.
     *
     * Type: String
     * Values: "home", "subscriptions", "trending", "library"
     * Default: "home"
     */
    const val START_PAGE = "pref_start_page"

    /**
     * Key for showing search suggestions.
     *
     * Type: Boolean
     * Default: true
     */
    const val SHOW_SUGGESTIONS = "pref_show_suggestions"

    /**
     * Key for showing comments section.
     *
     * Type: Boolean
     * Default: true
     */
    const val SHOW_COMMENTS = "pref_show_comments"

    /**
     * Key for enabling search history.
     *
     * Type: Boolean
     * Default: true
     */
    const val ENABLE_SEARCH_HISTORY = "pref_search_history"

    /**
     * Key for enabling watch history.
     *
     * Type: Boolean
     * Default: true
     */
    const val ENABLE_WATCH_HISTORY = "pref_watch_history"

    /**
     * Key for swipe-to-close gesture on video player.
     *
     * Type: Boolean
     * Default: true
     */
    const val SWIPE_TO_CLOSE = "pref_swipe_to_close"

    /**
     * Key for double-tap to seek gesture.
     *
     * Type: Boolean
     * Default: true
     */
    const val DOUBLE_TAP_TO_SEEK = "pref_double_tap_seek"

    /**
     * Key for seek duration in seconds when using seek gestures.
     *
     * Type: Int
     * Range: 5-30 seconds
     * Default: 10
     */
    const val SEEK_DURATION = "pref_seek_duration"

    /* ========================================================================
       NOTIFICATIONS
       ======================================================================== */

    /**
     * Key for enabling notifications globally.
     *
     * Type: Boolean
     * Default: true
     */
    const val ENABLE_NOTIFICATIONS = "pref_enable_notifications"

    /**
     * Key for notifications about new videos from subscriptions.
     *
     * Type: Boolean
     * Default: true
     */
    const val NOTIFY_NEW_VIDEOS = "pref_notify_new_videos"

    /**
     * Key for notifications about live streams.
     *
     * Type: Boolean
     * Default: true
     */
    const val NOTIFY_LIVE_STREAMS = "pref_notify_live_streams"

    /**
     * Key for notification sound preference.
     *
     * Type: Boolean
     * Default: true
     */
    const val NOTIFICATION_SOUND = "pref_notification_sound"

    /**
     * Key for notification vibration preference.
     *
     * Type: Boolean
     * Default: false
     */
    const val NOTIFICATION_VIBRATE = "pref_notification_vibrate"

    /* ========================================================================
       TV SPECIFIC
       ======================================================================== */

    /**
     * Key for tracking TV first launch state.
     *
     * Type: Boolean
     * Default: true
     */
    const val TV_FIRST_LAUNCH = "pref_tv_first_launch"

    /**
     * Key for TV remote control layout preference.
     *
     * Type: String
     * Values: "dpad", "touchpad"
     * Default: "dpad"
     */
    const val TV_REMOTE_LAYOUT = "pref_tv_remote_layout"

    /**
     * Key for enabling voice search on TV.
     *
     * Type: Boolean
     * Default: true
     */
    const val TV_VOICE_SEARCH = "pref_tv_voice_search"

    /**
     * Key for auto-play delay on TV in seconds.
     *
     * Type: Int
     * Range: 0-30 seconds
     * Default: 5
     */
    const val TV_AUTOPLAY_DELAY = "pref_tv_autoplay_delay"

    /**
     * Key for TV sidebar position.
     *
     * Type: String
     * Values: "left", "right"
     * Default: "left"
     */
    const val TV_SIDEBAR_POSITION = "pref_tv_sidebar_position"

    /* ========================================================================
       PRIVACY & SECURITY
       ======================================================================== */

    /**
     * Key for incognito mode (disables history and tracking).
     *
     * Type: Boolean
     * Default: false
     */
    const val INCOGNITO_MODE = "pref_incognito_mode"

    /**
     * Key for clearing history when exiting the app.
     *
     * Type: Boolean
     * Default: false
     */
    const val CLEAR_HISTORY_ON_EXIT = "pref_clear_history_on_exit"

    /**
     * Key for requiring authentication to access the app.
     *
     * Type: Boolean
     * Default: false
     */
    const val REQUIRE_AUTHENTICATION = "pref_require_auth"

    /**
     * Key for blocking tracking and analytics.
     *
     * Type: Boolean
     * Default: true
     */
    const val BLOCK_TRACKING = "pref_block_tracking"

    /* ========================================================================
       ADVANCED
       ======================================================================== */

    /**
     * Key for enabling experimental features.
     *
     * Type: Boolean
     * Default: false
     */
    const val EXPERIMENTAL_FEATURES = "pref_experimental_features"

    /**
     * Key for using legacy player implementation.
     *
     * Type: Boolean
     * Default: false
     */
    const val USE_LEGACY_PLAYER = "pref_use_legacy_player"

    /**
     * Key for forcing old API endpoints.
     *
     * Type: Boolean
     * Default: false
     */
    const val FORCE_OLD_API = "pref_force_old_api"

    /**
     * Key for custom instance URL (for alternative backends).
     *
     * Type: String
     * Default: ""
     */
    const val CUSTOM_INSTANCE = "pref_custom_instance"

    /**
     * Key for hardware acceleration in video playback.
     *
     * Type: Boolean
     * Default: true
     */
    const val HARDWARE_ACCELERATION = "pref_hardware_acceleration"

    /**
     * Key for reducing UI animations.
     *
     * Type: Boolean
     * Default: false
     */
    const val REDUCE_ANIMATIONS = "pref_reduce_animations"

    /**
     * Key for limiting background processes to save battery.
     *
     * Type: Boolean
     * Default: false
     */
    const val LIMIT_BACKGROUND_PROCESSES = "pref_limit_bg_processes"

    /* ========================================================================
       FIRST RUN & ONBOARDING
       ======================================================================== */

    /**
     * Key for tracking if this is the first app launch.
     *
     * Type: Boolean
     * Default: true
     */
    const val FIRST_LAUNCH = "pref_first_launch"

    /**
     * Key for tracking if onboarding has been completed.
     *
     * Type: Boolean
     * Default: false
     */
    const val ONBOARDING_COMPLETED = "pref_onboarding_completed"

    /**
     * Key for storing the last known app version code.
     *
     * Type: Int
     * Default: 0
     */
    const val APP_VERSION_CODE = "pref_app_version_code"

    /**
     * Key for timestamp of last update check.
     *
     * Type: Long
     * Default: 0
     */
    const val LAST_UPDATE_CHECK = "pref_last_update_check"

    /* ========================================================================
       DEFAULT VALUES
       ======================================================================== */

    /**
     * Default values for all preferences in the application.
     *
     * This nested object provides recommended default values for preferences, ensuring consistent
     * initialization across the app. These values are used when preferences haven't been set yet
     * or when resetting to defaults.
     *
     * ## Usage Pattern
     * ```kotlin
     * val theme = settings.getString(
     *     Preferences.THEME,
     *     Preferences.Defaults.THEME
     * )
     * ```
     *
     * ## Benefits
     * - **Type Safety**: Compile-time checking of default value types
     * - **Discoverability**: Easy to find recommended defaults
     * - **Consistency**: Same defaults used throughout the app
     * - **Maintainability**: Centralized location for default value changes
     *
     * @see PreferenceHelper for reading preferences with these defaults
     */
    object Defaults {
        // Theme
        /** Default theme mode: system default */
        const val THEME = "system"

        /** Default dynamic color state: disabled */
        const val DYNAMIC_COLOR = false

        // Grid columns
        /** Default grid columns in portrait: 2 */
        const val GRID_COLUMNS_PORTRAIT = 2

        /** Default grid columns in landscape: 3 */
        const val GRID_COLUMNS_LANDSCAPE = 3

        /** Default grid columns on TV: 4 */
        const val GRID_COLUMNS_TV = 4

        // UI scale
        /** Default UI scale multiplier: 1.0 (100%) */
        const val UI_SCALE = 1.0f

        /** Default font size multiplier: 1.0 (100%) */
        const val FONT_SIZE_MULTIPLIER = 1.0f

        // Playback
        /** Default auto-play state: enabled */
        const val AUTO_PLAY = true

        /** Default auto-play next: disabled */
        const val AUTO_PLAY_NEXT = false

        /** Default background playback: disabled */
        const val BACKGROUND_PLAYBACK = false

        /** Default picture-in-picture: enabled */
        const val PICTURE_IN_PICTURE = true

        /** Default remember playback position: enabled */
        const val REMEMBER_PLAYBACK_POSITION = true

        // Quality
        /** Default video resolution: 720p */
        const val DEFAULT_RESOLUTION = "720p"

        /** Default TV resolution: 1080p */
        const val DEFAULT_RESOLUTION_TV = "1080p"

        /** Default image quality: high */
        const val IMAGE_QUALITY = "high"

        // Data
        /** Default data saving mode: disabled */
        const val DATA_SAVING_MODE = false

        /** Default download over Wi-Fi only: enabled */
        const val DOWNLOAD_OVER_WIFI_ONLY = true

        /** Default preload thumbnails: enabled */
        const val PRELOAD_THUMBNAILS = true

        // Seek
        /** Default seek duration: 10 seconds */
        const val SEEK_DURATION = 10

        /** Default double-tap to seek: enabled */
        const val DOUBLE_TAP_TO_SEEK = true

        // TV
        /** Default TV auto-play delay: 5 seconds */
        const val TV_AUTOPLAY_DELAY = 5

        /** Default TV UI mode: lean_back */
        const val TV_UI_MODE = "lean_back"

        /** Default TV focus animation: enabled */
        const val TV_FOCUS_ANIMATION = true

        // Privacy
        /** Default incognito mode: disabled */
        const val INCOGNITO_MODE = false

        /** Default clear history on exit: disabled */
        const val CLEAR_HISTORY_ON_EXIT = false

        /** Default block tracking: enabled */
        const val BLOCK_TRACKING = true

        // Cache
        /** Default cache size: 200 MB */
        const val CACHE_SIZE = 200

        /** Default metadata cache expiry: 3600 seconds (1 hour) */
        const val METADATA_CACHE_EXPIRY = 3600
    }
}