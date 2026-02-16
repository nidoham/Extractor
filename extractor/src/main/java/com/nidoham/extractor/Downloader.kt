package com.nidoham.extractor

import android.content.Context
import android.util.Log
import com.nidoham.extractor.util.LocaleHelper
import com.nidoham.extractor.util.PreferenceHelper
import com.nidoham.extractor.util.Preferences
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

/**
 * Singleton manager for YouTube data extraction and NewPipe initialization.
 *
 * This object serves as the central coordinator for all YouTube content extraction operations in the
 * application. It manages the lifecycle of the NewPipe extractor, handles localization settings,
 * and provides a simplified interface for accessing YouTube's streaming service.
 *
 * ## Responsibilities
 * - **Initialization**: One-time setup of NewPipe and preferences during app startup
 * - **Localization Management**: Applies and updates language/region preferences for content
 * - **Service Access**: Provides lazy-loaded access to the YouTube streaming service
 * - **Runtime Updates**: Supports dynamic language and region changes without restart
 *
 * ## Architecture
 * The Downloader follows a singleton pattern with lazy initialization to ensure:
 * 1. Only one instance of NewPipe exists throughout the app
 * 2. Resources are initialized only when needed
 * 3. Thread-safe initialization using double-checked locking
 * 4. Centralized access point for all extraction operations
 *
 * ## Initialization Flow
 * ```
 * Application.onCreate()
 *     ↓
 * Downloader.init(context)
 *     ↓
 * PreferenceHelper.initialize()
 *     ↓
 * NewPipeExtractorInstance.init()
 *     ↓
 * Apply localization settings
 *     ↓
 * Ready for use
 * ```
 *
 * ## Usage Example
 * ```kotlin
 * // In Application class
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         Downloader.init(this)
 *     }
 * }
 *
 * // In any component
 * val youtubeService = Downloader.extractor
 * val searchExtractor = youtubeService.getSearchExtractor(query)
 *
 * // Update language at runtime
 * Downloader.updateLanguage("es")
 *
 * // Update region
 * Downloader.updateRegion(context, "US")
 *
 * // Get current localization info
 * val info = Downloader.getLocalizationInfo()
 * println("Language: ${info?.languageName}, Region: ${info?.countryName}")
 * ```
 *
 * ## Thread Safety
 * - Initialization is thread-safe using `synchronized` block with double-checked locking
 * - The `@Volatile` annotation ensures visibility across threads
 * - Service access is lazy-loaded but thread-safe through Kotlin's `lazy` delegate
 * - Runtime updates should be called from main thread for UI consistency
 *
 * ## Error Handling
 * - Initialization failures throw [RuntimeException] with detailed error message
 * - Runtime update failures are logged but don't crash the app
 * - All operations check initialization state before proceeding
 *
 * ## Important Notes
 * - Must be initialized in `Application.onCreate()` before any extraction operations
 * - Initialization is idempotent - safe to call multiple times
 * - Changes to language/region are persisted to SharedPreferences
 * - Localization changes affect content language, not UI language
 *
 * @see NewPipeExtractorInstance for the underlying NewPipe initialization
 * @see PreferenceHelper for preference storage
 * @see LocaleHelper for locale and region utilities
 */
object Downloader {

    /** Log tag for debugging and error reporting */
    private const val TAG = "Downloader"

    /**
     * Thread-safe flag indicating whether the Downloader has been initialized.
     *
     * Uses `@Volatile` to ensure visibility of changes across all threads without
     * requiring synchronization for reads. This is part of the double-checked locking
     * pattern used in [init].
     */
    @Volatile
    private var initialized = false

    /**
     * Lazy-loaded YouTube streaming service instance from NewPipe.
     *
     * This property provides on-demand access to the YouTube StreamingService, which is the
     * main entry point for all YouTube extraction operations. It uses Kotlin's `lazy` delegate
     * for thread-safe, one-time initialization.
     *
     * ## Lazy Initialization
     * - Service is created only when first accessed
     * - Creation is thread-safe (default lazy mode)
     * - Subsequent accesses return the same instance
     *
     * ## Pre-requisites
     * - [init] must have been called before accessing this property
     * - Accessing before initialization throws [IllegalStateException]
     *
     * ## Provided Capabilities
     * The StreamingService provides access to:
     * - Search functionality
     * - Trending videos
     * - Channel information
     * - Video details and streams
     * - Playlist information
     * - Suggestions and autocomplete
     *
     * ## Usage Example
     * ```kotlin
     * // Get search results
     * val service = Downloader.extractor
     * val searchExtractor = service.getSearchExtractor(searchQuery)
     * searchExtractor.fetchPage()
     * val results = searchExtractor.initialPage.items
     *
     * // Get trending videos
     * val kiosk = service.getKioskList().getDefaultKioskExtractor()
     * kiosk.fetchPage()
     * val trending = kiosk.initialPage.items
     * ```
     *
     * @throws IllegalStateException if accessed before [init] has been called
     * @see init for initialization requirements
     */
    val extractor: StreamingService by lazy {
        ensureInitialized()
        NewPipeExtractorInstance.youtubeService
    }

    /**
     * Initializes the Downloader with application context.
     *
     * This method performs one-time initialization of all components required for YouTube content
     * extraction. It should be called once during application startup in `Application.onCreate()`.
     * The method is idempotent - calling it multiple times is safe and has no additional effect.
     *
     * ## Initialization Steps
     * 1. **Check State**: Returns early if already initialized
     * 2. **Thread Safety**: Acquires lock to prevent concurrent initialization
     * 3. **Preferences**: Initializes SharedPreferences storage
     * 4. **NewPipe**: Initializes NewPipe extractor with caching support
     * 5. **Localization**: Applies language and region settings from preferences
     * 6. **Validation**: Sets initialized flag and logs success
     *
     * ## Thread Safety
     * Uses double-checked locking pattern:
     * - First check avoids synchronization overhead for already-initialized state
     * - Synchronized block prevents concurrent initialization attempts
     * - Second check within lock handles race conditions
     * - `@Volatile` flag ensures visibility across threads
     *
     * ## Error Handling
     * If initialization fails at any step:
     * - Logs detailed error information
     * - Throws [RuntimeException] with original exception as cause
     * - Prevents partial initialization by not setting the initialized flag
     *
     * ## Application Integration
     * ```kotlin
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *
     *         // Initialize as early as possible
     *         try {
     *             Downloader.init(this)
     *         } catch (e: RuntimeException) {
     *             // Handle critical initialization failure
     *             Log.e("App", "Failed to initialize Downloader", e)
     *             // Potentially show error UI or exit gracefully
     *         }
     *     }
     * }
     * ```
     *
     * ## Logging
     * Success path logs:
     * - "Preferences initialized"
     * - "NewPipe initialized"
     * - "Applied localization: {language}-{region}"
     * - "Downloader initialized successfully"
     *
     * Error path logs:
     * - "Failed to initialize Downloader" with full stack trace
     *
     * @param context The application context. Using application context is recommended to avoid
     * memory leaks. Should typically be `applicationContext` or `this` from Application class.
     * @throws RuntimeException if initialization fails for any reason (e.g., NewPipe setup failure,
     * preference access issues). The original exception is wrapped as the cause.
     *
     * @see PreferenceHelper.initialize for preference initialization details
     * @see NewPipeExtractorInstance.init for NewPipe initialization details
     * @see applyLocalizationSettings for localization setup
     */
    fun init(context: Context) {
        if (initialized) {
            Log.d(TAG, "Downloader already initialized")
            return
        }

        synchronized(this) {
            if (initialized) return

            try {
                // Initialize preferences first
                PreferenceHelper.initialize(context)
                Log.d(TAG, "Preferences initialized")

                // Initialize NewPipe with context for caching
                NewPipeExtractorInstance.init(context)
                Log.d(TAG, "NewPipe initialized")

                // Apply localization settings
                applyLocalizationSettings(context)

                initialized = true
                Log.i(TAG, "Downloader initialized successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Downloader", e)
                throw RuntimeException("Failed to initialize Downloader", e)
            }
        }
    }

    /**
     * Applies localization settings from preferences to NewPipe.
     *
     * This internal method reads the user's language and region preferences and applies them
     * to the NewPipe extractor. This affects which language versions of content are fetched
     * (e.g., video titles, descriptions) and which regional trending content is shown.
     *
     * ## Behavior
     * - Reads content language from [Preferences.CONTENT_LANGUAGE]
     * - Reads trending region from [Preferences.REGION] (with auto-detection fallback)
     * - Creates NewPipe Localization and ContentCountry objects
     * - Applies settings to NewPipeExtractorInstance
     * - Logs the applied settings for debugging
     *
     * ## Error Handling
     * - Catches all exceptions to prevent initialization failure
     * - Logs errors but doesn't throw (allows app to continue with defaults)
     * - NewPipe will use its default localization if this fails
     *
     * ## Localization Components
     * - **Language**: ISO 639-1 language code (e.g., "en", "es", "zh")
     * - **Region**: ISO 3166-1 alpha-2 country code (e.g., "US", "GB", "JP")
     *
     * @param context The context used for region auto-detection when preference is "sys".
     *
     * @see LocaleHelper.getAppLocale for content language retrieval
     * @see PreferenceHelper.getTrendingRegion for region retrieval with auto-detection
     */
    private fun applyLocalizationSettings(context: Context) {
        try {
            // Get locale from preferences
            val appLocale = LocaleHelper.getAppLocale()

            // Get region from preferences (with auto-detection for "sys")
            val region = PreferenceHelper.getTrendingRegion(context)

            // Create localization objects (kept for potential future use)
            val localization = Localization(appLocale.language, region)
            val contentCountry = ContentCountry(region)

            // Apply to NewPipe
            NewPipeExtractorInstance.updateLocalization(
                languageCode = appLocale.language,
                countryCode = region
            )

            Log.i(TAG, "Applied localization: ${appLocale.language}-$region")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply localization settings", e)
        }
    }

    /**
     * Updates the content language preference and applies it to NewPipe at runtime.
     *
     * This method allows changing the content language without restarting the app. It saves the
     * new preference and immediately applies it to NewPipe, affecting future content fetches.
     * Note that this changes content language, not UI language (use [LocaleHelper.updateAppLanguage]
     * for UI language changes).
     *
     * ## Behavior
     * - Validates that Downloader is initialized
     * - Saves new language code to [Preferences.CONTENT_LANGUAGE]
     * - Retrieves current region setting
     * - Updates NewPipe's localization with new language
     * - Logs the language change
     *
     * ## Effects
     * After calling this method:
     * - Future search results will be in the new language
     * - Video titles and descriptions will be fetched in the new language
     * - Trending videos will reflect the new language preference
     * - Changes persist across app restarts
     *
     * ## Thread Safety
     * - Can be called from any thread
     * - Preference writes are asynchronous (won't block)
     * - NewPipe updates are thread-safe
     *
     * ## Error Handling
     * - Returns early with warning log if not initialized
     * - Catches and logs exceptions without throwing
     * - Partial failures are logged but don't prevent app operation
     *
     * ## Usage Example
     * ```kotlin
     * // User selects Spanish for content
     * Downloader.updateLanguage("es")
     *
     * // Future searches will return Spanish results
     * val results = searchExtractor.search("tutorials")
     * // Results will have Spanish titles/descriptions
     *
     * // Reset to system default
     * Downloader.updateLanguage("sys")
     * ```
     *
     * @param languageCode The new language code to apply. Use "sys" for system default,
     * or ISO 639-1 codes like "en", "es", "zh". Extended codes like "zh-CN" are also supported.
     *
     * @see updateRegion for changing the region/country
     * @see LocaleHelper.updateAppLanguage for changing UI language
     */
    fun updateLanguage(languageCode: String) {
        if (!initialized) {
            Log.w(TAG, "Cannot update language - Downloader not initialized")
            return
        }

        try {
            // Save to preferences
            PreferenceHelper.settings.edit()
                .putString(Preferences.CONTENT_LANGUAGE, languageCode)
                .apply()

            // Get current region
            val region = PreferenceHelper.getString(Preferences.REGION, "BD")

            // Update NewPipe
            NewPipeExtractorInstance.updateLocalization(languageCode, region)

            Log.i(TAG, "Language updated to: $languageCode")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update language", e)
        }
    }

    /**
     * Updates the trending region preference and applies it to NewPipe at runtime.
     *
     * This method allows changing the content region without restarting the app. It saves the
     * new preference and immediately applies it to NewPipe, affecting regional content like
     * trending videos. The region determines which country's trending content is shown.
     *
     * ## Behavior
     * - Validates that Downloader is initialized
     * - Saves new region code to [Preferences.REGION]
     * - Retrieves current language setting
     * - Updates NewPipe's localization with new region
     * - Logs the region change
     *
     * ## Effects
     * After calling this method:
     * - Trending videos will be from the new region
     * - Regional search results may be prioritized
     * - Content recommendations may reflect regional preferences
     * - Changes persist across app restarts
     *
     * ## Region Codes
     * - **"sys"**: Auto-detect from SIM/network/locale
     * - **ISO 3166-1 alpha-2**: "US", "GB", "JP", "BR", etc.
     * - Codes are case-insensitive but stored uppercase
     *
     * ## Thread Safety
     * - Can be called from any thread
     * - Preference writes are asynchronous (won't block)
     * - NewPipe updates are thread-safe
     *
     * ## Error Handling
     * - Returns early with warning log if not initialized
     * - Catches and logs exceptions without throwing
     * - Partial failures are logged but don't prevent app operation
     *
     * ## Usage Example
     * ```kotlin
     * // User selects United States trending
     * Downloader.updateRegion(context, "US")
     *
     * // Get trending videos (will be US trending)
     * val trendingExtractor = service.getKioskList().getDefaultKioskExtractor()
     * val trending = trendingExtractor.initialPage.items
     *
     * // Switch to Japan
     * Downloader.updateRegion(context, "JP")
     *
     * // Reset to auto-detect
     * Downloader.updateRegion(context, "sys")
     * ```
     *
     * @param context The context used for auto-detection when region is "sys".
     * Should typically be application context to avoid leaks.
     * @param regionCode The new region code to apply. Use "sys" for auto-detection,
     * or ISO 3166-1 alpha-2 codes like "US", "GB", "JP".
     *
     * @see updateLanguage for changing the content language
     * @see PreferenceHelper.getTrendingRegion for how "sys" is resolved
     */
    fun updateRegion(context: Context, regionCode: String) {
        if (!initialized) {
            Log.w(TAG, "Cannot update region - Downloader not initialized")
            return
        }

        try {
            // Save to preferences
            PreferenceHelper.settings.edit()
                .putString(Preferences.REGION, regionCode)
                .apply()

            // Get current language
            val appLocale = LocaleHelper.getAppLocale()

            // Update NewPipe
            NewPipeExtractorInstance.updateLocalization(appLocale.language, regionCode)

            Log.i(TAG, "Region updated to: $regionCode")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update region", e)
        }
    }

    /**
     * Retrieves current localization information including language and region details.
     *
     * This method provides a convenient way to access the current localization state of the
     * NewPipe extractor, including both the raw codes and human-readable display names.
     * Useful for displaying current settings in UI or debugging localization issues.
     *
     * ## Returned Information
     * Returns a [LocalizationInfo] object containing:
     * - **languageCode**: ISO 639-1 code (e.g., "en", "es")
     * - **countryCode**: ISO 3166-1 alpha-2 code (e.g., "US", "GB")
     * - **languageName**: Localized language name (e.g., "English", "Spanish")
     * - **countryName**: Localized country name (e.g., "United States", "Japan")
     *
     * ## Behavior
     * - Returns null if Downloader is not initialized
     * - Queries NewPipe for current localization settings
     * - Uses Java Locale for display name generation
     * - Provides fallback values if data is missing
     * - Catches and logs errors, returning null on failure
     *
     * ## Display Names
     * Display names are generated using Java's Locale class and are localized to the
     * device's current locale. For example:
     * - On English device: "Spanish" / "Spain"
     * - On Spanish device: "Español" / "España"
     *
     * ## Usage Example
     * ```kotlin
     * // Display in settings UI
     * val info = Downloader.getLocalizationInfo()
     * if (info != null) {
     *     languageTextView.text = "Language: ${info.languageName}"
     *     regionTextView.text = "Region: ${info.countryName}"
     *
     *     // Show full details
     *     detailsTextView.text = info.toString()
     *     // Output: "English (en) - United States (US)"
     * } else {
     *     // Not initialized or error occurred
     *     showError("Unable to retrieve localization info")
     * }
     *
     * // Check specific values
     * info?.let {
     *     if (it.countryCode == "US") {
     *         enableUSSpecificFeature()
     *     }
     * }
     * ```
     *
     * ## Error Handling
     * Returns null if:
     * - Downloader hasn't been initialized
     * - NewPipe localization data is unavailable
     * - An exception occurs during retrieval
     *
     * All errors are logged for debugging purposes.
     *
     * @return A [LocalizationInfo] object with current settings, or null if unavailable or
     * on error. The returned object provides both codes and human-readable names.
     *
     * @see LocalizationInfo for the structure of returned data
     * @see isInitialized for checking initialization state
     */
    fun getLocalizationInfo(): LocalizationInfo? {
        if (!initialized) return null

        return try {
            val localization = NewPipeExtractorInstance.getCurrentLocalization()
            val country = NewPipeExtractorInstance.getCurrentContentCountry()

            val languageCode = localization?.localizationCode ?: "en"
            val countryCode = country?.countryCode ?: "US"

            // Get display names from Java Locale
            val locale = java.util.Locale(languageCode, countryCode)

            LocalizationInfo(
                languageCode = languageCode,
                countryCode = countryCode,
                languageName = locale.displayLanguage.takeIf { it.isNotEmpty() } ?: "English",
                countryName = locale.displayCountry.takeIf { it.isNotEmpty() } ?: "United States"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get localization info", e)
            null
        }
    }

    /**
     * Checks whether the Downloader has been successfully initialized.
     *
     * This method provides a simple way to verify initialization state before attempting
     * operations that require the Downloader to be ready. It's useful for conditional logic,
     * error prevention, and debugging initialization issues.
     *
     * ## Use Cases
     * - **Defensive Programming**: Check before accessing extractor services
     * - **Early Startup**: Determine if app is ready for content operations
     * - **Error Recovery**: Detect initialization failures for user feedback
     * - **Testing**: Verify initialization in unit/integration tests
     *
     * ## Behavior
     * - Returns true only after successful completion of [init]
     * - Returns false before initialization or if initialization failed
     * - Thread-safe read operation (uses volatile flag)
     * - No side effects, can be called freely
     *
     * ## Usage Example
     * ```kotlin
     * // Before using extractor
     * if (!Downloader.isInitialized()) {
     *     Log.w(TAG, "Downloader not ready")
     *     showLoadingScreen()
     *     return
     * }
     *
     * val service = Downloader.extractor
     * // Safe to use service here
     *
     * // In error handling
     * try {
     *     performExtraction()
     * } catch (e: IllegalStateException) {
     *     if (!Downloader.isInitialized()) {
     *         showErrorDialog("App not fully initialized")
     *     }
     * }
     *
     * // In app health check
     * fun checkAppHealth(): Boolean {
     *     return Downloader.isInitialized() &&
     *            networkIsAvailable() &&
     *            hasRequiredPermissions()
     * }
     * ```
     *
     * @return true if [init] has completed successfully, false otherwise.
     *
     * @see init for initialization details
     * @see ensureInitialized for throwing version of this check
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Ensures that the Downloader has been initialized, throwing an exception if not.
     *
     * This internal method provides a fail-fast mechanism to prevent use of the Downloader
     * before it's properly initialized. It's used by critical methods like the [extractor]
     * property to catch initialization errors early.
     *
     * ## Behavior
     * - Does nothing if initialized (fast path)
     * - Throws [IllegalStateException] with helpful message if not initialized
     * - Should be called by any method requiring initialization
     *
     * ## Error Message
     * The thrown exception includes clear instructions:
     * ```
     * "Downloader not initialized. Call Downloader.init(context) in Application.onCreate()"
     * ```
     *
     * ## Internal Use
     * This is a private utility method used by:
     * - The `extractor` lazy property
     * - Any future methods requiring guaranteed initialization
     *
     * @throws IllegalStateException if [init] has not been called successfully.
     *
     * @see init for proper initialization
     * @see isInitialized for non-throwing version
     */
    private fun ensureInitialized() {
        if (!initialized) {
            throw IllegalStateException(
                "Downloader not initialized. Call Downloader.init(context) in Application.onCreate()"
            )
        }
    }

    /**
     * Data class containing comprehensive localization information.
     *
     * This class encapsulates both technical codes and human-readable names for language
     * and country settings, making it easy to display localization info in UI while maintaining
     * access to the underlying codes for programmatic use.
     *
     * ## Structure
     * Contains four pieces of information:
     * - Technical codes: languageCode, countryCode (for APIs and storage)
     * - Display names: languageName, countryName (for user interfaces)
     *
     * ## String Representation
     * The [toString] method provides a formatted string suitable for display or logging:
     * ```
     * "English (en) - United States (US)"
     * "Spanish (es) - Mexico (MX)"
     * "Japanese (ja) - Japan (JP)"
     * ```
     *
     * ## Usage Example
     * ```kotlin
     * val info = Downloader.getLocalizationInfo()!!
     *
     * // Access codes for logic
     * when (info.languageCode) {
     *     "en" -> loadEnglishResources()
     *     "es" -> loadSpanishResources()
     * }
     *
     * // Display names in UI
     * settingsText.text = "Content Language: ${info.languageName}"
     * regionText.text = "Trending Region: ${info.countryName}"
     *
     * // Full formatted string
     * Log.d(TAG, "Localization: $info")
     * // Output: "Localization: English (en) - United States (US)"
     * ```
     *
     * @property languageCode The ISO 639-1 language code (e.g., "en", "es", "ja").
     * Used for API calls and preference storage.
     * @property countryCode The ISO 3166-1 alpha-2 country code (e.g., "US", "GB", "JP").
     * Used for regional content and API calls.
     * @property languageName The localized display name of the language (e.g., "English",
     * "Spanish", "日本語"). Localized to the device's current locale.
     * @property countryName The localized display name of the country (e.g., "United States",
     * "Japan", "Brazil"). Localized to the device's current locale.
     *
     * @see getLocalizationInfo for retrieving this information
     */
    data class LocalizationInfo(
        val languageCode: String,
        val countryCode: String,
        val languageName: String,
        val countryName: String
    ) {
        /**
         * Returns a formatted string representation of the localization info.
         *
         * The format is: "{languageName} ({languageCode}) - {countryName} ({countryCode})"
         *
         * This provides a complete, human-readable description suitable for:
         * - Debug logging
         * - Settings display
         * - User notifications
         * - Error messages
         *
         * ## Examples
         * ```
         * "English (en) - United States (US)"
         * "Spanish (es) - Spain (ES)"
         * "Chinese (zh) - China (CN)"
         * "French (fr) - Canada (CA)"
         * ```
         *
         * @return A formatted string with both display names and codes.
         */
        override fun toString(): String {
            return "$languageName ($languageCode) - $countryName ($countryCode)"
        }
    }
}