package com.nidoham.extractor.util

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit

/**
 * Centralized helper for managing application SharedPreferences with type-safe access.
 *
 * This singleton object provides a simplified interface for reading and writing SharedPreferences
 * throughout the application. It handles preference initialization, provides convenience methods
 * for common operations, and maintains references to both regular and sensitive data storage.
 *
 * ## Key Features
 * - **Centralized Access**: Single point of access for all app preferences
 * - **Type Safety**: Provides type-specific getter/setter methods
 * - **Lazy Initialization**: Must be initialized before use, preventing NPE issues
 * - **Dual Storage**: Supports both regular and auth-specific preferences
 * - **Convenience Methods**: Higher-level methods for common preference operations
 *
 * ## Architecture
 * The helper maintains two SharedPreferences instances:
 * 1. **settings**: Default SharedPreferences for regular app settings
 * 2. **authSettings**: Separate preferences file for sensitive authentication data (commented out)
 *
 * ## Initialization Pattern
 * ```kotlin
 * class MyApplication : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         PreferenceHelper.initialize(this)
 *     }
 * }
 * ```
 *
 * ## Usage Examples
 * ```kotlin
 * // Reading preferences
 * val theme = PreferenceHelper.getString(Preferences.THEME, "system")
 * val region = PreferenceHelper.getTrendingRegion(context)
 *
 * // Writing preferences
 * PreferenceHelper.putString(Preferences.THEME, "dark")
 *
 * // Special operations
 * PreferenceHelper.saveErrorLog("Error details...")
 * val log = PreferenceHelper.getErrorLog()
 * ```
 *
 * ## Thread Safety
 * - Read operations are thread-safe (SharedPreferences guarantees this)
 * - Write operations use `edit { }` which handles commit/apply automatically
 * - The `settings` property must be initialized on the main thread during app startup
 *
 * ## Important Notes
 * - Must call [initialize] before accessing any preferences
 * - Accessing [settings] before initialization throws IllegalStateException
 * - All preference keys should be defined in the [Preferences] object
 *
 * @see Preferences for all available preference keys and defaults
 * @see LocaleHelper for locale-specific preference handling
 */
object PreferenceHelper {

    /**
     * Default SharedPreferences instance for regular application settings.
     *
     * This property provides access to the application's default SharedPreferences, which stores
     * all user settings, feature flags, and configuration values. It must be initialized via
     * [initialize] before use.
     *
     * ## Usage
     * - **Read Access**: Available to all code through this public property
     * - **Write Access**: Use [putString] and other helper methods for consistency
     * - **Direct Access**: Can be used for advanced operations not covered by helper methods
     *
     * ## Error Handling
     * Accessing this property before [initialize] has been called will throw:
     * ```
     * kotlin.UninitializedPropertyAccessException: lateinit property settings has not been initialized
     * ```
     *
     * @throws UninitializedPropertyAccessException if accessed before [initialize] is called
     */
    lateinit var settings: SharedPreferences
        private set

    /**
     * SharedPreferences instance for sensitive authentication data.
     *
     * This private property is intended to store sensitive information like authentication tokens,
     * API keys, or user credentials in a separate preferences file with restricted access.
     * Currently commented out but can be enabled for apps requiring secure storage.
     *
     * ## Security Considerations
     * - Uses `MODE_PRIVATE` to restrict access to this app only
     * - Consider using Android Keystore for highly sensitive data
     * - Should be encrypted for apps handling critical security data
     *
     * ## Future Use
     * Uncomment and initialize in [initialize] method when authentication features are needed.
     */
    private lateinit var authSettings: SharedPreferences

    /**
     * Character set used for generating random user IDs.
     *
     * This constant defines the pool of characters available for generating unique user identifiers
     * for services like SponsorBlock. The set includes uppercase letters and numbers for
     * URL-safe identifiers.
     *
     * ## Character Set
     * - Uppercase letters: A-Z (26 characters)
     * - Numbers: 1-9, 0 (10 characters)
     * - Total pool: 36 characters
     *
     * ## Usage
     * ```kotlin
     * fun generateUserId(length: Int = 16): String {
     *     return (1..length)
     *         .map { USER_ID_CHARS.random() }
     *         .joinToString("")
     * }
     * ```
     */
    private const val USER_ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"

    /**
     * Initializes the PreferenceHelper with the application context.
     *
     * This method must be called once during application startup (typically in `Application.onCreate()`)
     * before any preferences can be accessed. It sets up the SharedPreferences instances that will
     * be used throughout the app's lifetime.
     *
     * ## Initialization Behavior
     * - Sets up default SharedPreferences using `PreferenceManager.getDefaultSharedPreferences()`
     * - Can be safely called multiple times (subsequent calls are no-ops)
     * - Should be called on the main thread during app startup
     * - Uses application context to avoid memory leaks
     *
     * ## Best Practices
     * ```kotlin
     * class MyApplication : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *         // Initialize preferences first, before other components
     *         PreferenceHelper.initialize(this)
     *         // ... other initialization
     *     }
     * }
     * ```
     *
     * ## Android Manifest
     * Ensure your Application class is registered:
     * ```xml
     * <application
     *     android:name=".MyApplication"
     *     ...>
     * ```
     *
     * @param context The application context. Should be application-scoped to avoid leaks.
     * Using `applicationContext` is recommended over activity contexts.
     */
    fun initialize(context: Context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context)
        // authSettings = context.getSharedPreferences(Preferences.AUTH_PREF_FILE, Context.MODE_PRIVATE)
    }

    /**
     * Stores a string value in SharedPreferences.
     *
     * This convenience method provides a simplified interface for writing string values to
     * preferences. It automatically handles the commit/apply operation using the more
     * efficient `apply()` method through Kotlin's `edit` extension.
     *
     * ## Behavior
     * - Uses `apply()` for asynchronous write (returns immediately)
     * - Overwrites any existing value for the given key
     * - Changes are persisted to disk asynchronously for better performance
     * - Thread-safe operation
     *
     * ## Usage Example
     * ```kotlin
     * PreferenceHelper.putString(Preferences.THEME, "dark")
     * PreferenceHelper.putString(Preferences.REGION, "US")
     * ```
     *
     * ## Performance Notes
     * - Asynchronous write doesn't block the calling thread
     * - Multiple consecutive calls are batched for efficiency
     * - Use `edit { putString(...) }.commit()` directly if synchronous write is required
     *
     * @param key The preference key. Should be one of the constants from [Preferences] object
     * for type safety and maintainability.
     * @param value The string value to store. Empty strings are valid; null should be avoided
     * (use empty string or remove the preference instead).
     *
     * @see getString for reading string values
     * @see Preferences for all available preference keys
     */
    fun putString(key: String, value: String) {
        settings.edit { putString(key, value) }
    }

    /**
     * Retrieves a string value from SharedPreferences with a default fallback.
     *
     * This method provides safe access to string preferences, returning a default value if
     * the preference doesn't exist or if the stored value is null.
     *
     * ## Null Safety
     * - If the preference doesn't exist, returns `defValue`
     * - If the stored value is null (should not happen with proper usage), returns `defValue`
     * - Never returns null, ensuring safe non-null string handling
     *
     * ## Usage Example
     * ```kotlin
     * val theme = PreferenceHelper.getString(Preferences.THEME, Preferences.Defaults.THEME)
     * val language = PreferenceHelper.getString(Preferences.APP_LANGUAGE, "sys")
     * ```
     *
     * ## Type Safety
     * While this method accepts nullable keys, it's recommended to always use non-null keys
     * from the [Preferences] object to avoid accidental null pointer issues.
     *
     * @param key The preference key to look up. Should be one of the constants from [Preferences]
     * object. Can technically be null but this is not recommended.
     * @param defValue The default value to return if the preference doesn't exist or is null.
     * Should match the expected type for the preference key.
     * @return The stored string value, or `defValue` if not found or null. Never returns null.
     *
     * @see putString for writing string values
     * @see Preferences.Defaults for recommended default values
     */
    fun getString(key: String?, defValue: String): String {
        return settings.getString(key, defValue) ?: defValue
    }

    /**
     * Saves an error log message to preferences for debugging and crash reporting.
     *
     * This convenience method stores error information persistently, allowing the app to
     * retrieve and display or report errors even after a crash or restart. Useful for
     * debugging and crash analytics.
     *
     * ## Use Cases
     * - Storing exception stack traces for later reporting
     * - Preserving error state across app restarts
     * - Implementing a debug log viewer in the app
     * - Crash reporting when analytics SDK isn't available
     *
     * ## Behavior
     * - Overwrites any previously saved error log
     * - For multiple errors, consider appending with timestamps or using a different storage
     * - Logs are persisted until explicitly cleared or overwritten
     *
     * ## Usage Example
     * ```kotlin
     * try {
     *     riskyOperation()
     * } catch (e: Exception) {
     *     val log = "Error at ${System.currentTimeMillis()}: ${e.stackTraceToString()}"
     *     PreferenceHelper.saveErrorLog(log)
     *     // Optionally report to analytics
     * }
     * ```
     *
     * ## Storage Considerations
     * - SharedPreferences has size limits (~1MB per file recommended)
     * - Very large logs should use file storage or database instead
     * - Consider truncating old logs or rotating log files
     *
     * @param log The error log message to save. Typically includes exception details,
     * stack traces, timestamps, and context information.
     *
     * @see getErrorLog for retrieving the saved error log
     */
    fun saveErrorLog(log: String) {
        putString(Preferences.ERROR_LOG, log)
    }

    /**
     * Retrieves the most recently saved error log from preferences.
     *
     * This method fetches the error log that was previously saved using [saveErrorLog].
     * If no error log exists, it returns an empty string rather than null for safer handling.
     *
     * ## Behavior
     * - Returns the last saved error log message
     * - Returns empty string if no log has been saved
     * - Does not clear the log after retrieval (explicit clearing required)
     *
     * ## Use Cases
     * - Displaying error details in a debug screen
     * - Sending error reports to support or analytics
     * - Diagnosing crashes or issues reported by users
     * - Implementing a "send feedback" feature with error context
     *
     * ## Usage Example
     * ```kotlin
     * // In a debug screen
     * val errorLog = PreferenceHelper.getErrorLog()
     * if (errorLog.isNotEmpty()) {
     *     errorLogTextView.text = errorLog
     *     sendErrorReportButton.isEnabled = true
     * }
     *
     * // Send error report
     * fun sendErrorReport() {
     *     val log = PreferenceHelper.getErrorLog()
     *     analyticsService.reportError(log)
     *     // Clear after sending
     *     PreferenceHelper.putString(Preferences.ERROR_LOG, "")
     * }
     * ```
     *
     * @return The saved error log message, or an empty string if no error log exists.
     * Never returns null.
     *
     * @see saveErrorLog for storing error logs
     */
    fun getErrorLog(): String {
        return getString(Preferences.ERROR_LOG, "")
    }

    /**
     * Retrieves the trending region preference with automatic country detection fallback.
     *
     * This method provides intelligent region selection by using the user's explicit preference
     * when set, or automatically detecting their country when using system default. It's
     * specifically designed for features like YouTube trending content that require region codes.
     *
     * ## Behavior
     * - If user has set a specific region: Returns that region code (e.g., "US", "JP")
     * - If preference is `"sys"`: Automatically detects country using [LocaleHelper.getDetectedCountry]
     * - All returned codes are uppercase for consistency
     *
     * ## Detection Hierarchy (when using "sys")
     * 1. SIM card country (most reliable for mobile devices)
     * 2. Network country (good for roaming users)
     * 3. Locale country (based on device settings)
     * 4. Default to "UK" if all detection fails
     *
     * ## Use Cases
     * - Fetching region-specific trending videos
     * - Setting content region for YouTube API calls
     * - Initializing region-dependent features
     * - Providing localized content recommendations
     *
     * ## Usage Example
     * ```kotlin
     * // Fetch trending videos for user's region
     * val region = PreferenceHelper.getTrendingRegion(context)
     * val trendingVideos = youtubeService.getTrending(region)
     *
     * // Display in settings
     * val currentRegion = PreferenceHelper.getTrendingRegion(context)
     * regionTextView.text = "Current region: $currentRegion"
     * ```
     *
     * @param context The context used for automatic country detection when preference is "sys".
     * Should typically be application context to avoid memory leaks.
     * @return The uppercase ISO 3166-1 alpha-2 country code (e.g., "US", "GB", "JP").
     * Never returns null or lowercase codes.
     *
     * @see LocaleHelper.getDetectedCountry for the auto-detection mechanism
     * @see Preferences.REGION for the preference key
     */
    fun getTrendingRegion(context: Context): String {
        val regionPref = getString(Preferences.REGION, "sys")
        return if (regionPref == "sys") {
            LocaleHelper.getDetectedCountry(context).uppercase()
        } else {
            regionPref
        }
    }

    /**
     * Retrieves the current app language setting.
     *
     * This method returns the user's language preference for the application UI. The value
     * determines which language resources the app loads and displays.
     *
     * ## Return Values
     * - **"sys"**: Use system default language (device language)
     * - **ISO 639-1 code**: Specific language code (e.g., "en", "es", "zh")
     * - **Extended code**: Language with region (e.g., "zh-TW", "pt-BR")
     *
     * ## Behavior
     * - Returns the stored preference value directly
     * - Defaults to `"sys"` if no preference has been set
     * - Does not apply the language (use [LocaleHelper.setLocale] for that)
     *
     * ## Usage Example
     * ```kotlin
     * val currentLanguage = PreferenceHelper.getAppLanguage()
     *
     * when (currentLanguage) {
     *     "sys" -> showSystemLanguageIndicator()
     *     else -> showCustomLanguage(currentLanguage)
     * }
     * ```
     *
     * @return The language preference code. Returns `"sys"` by default if not set.
     * Never returns null.
     *
     * @see setAppLanguage for changing the language preference
     * @see LocaleHelper.updateAppLanguage for changing and applying the language
     */
    fun getAppLanguage(): String {
        return getString(Preferences.APP_LANGUAGE, "sys")
    }

    /**
     * Sets the app language preference and indicates whether it changed.
     *
     * This method updates the language preference in SharedPreferences and returns a boolean
     * indicating whether the value actually changed. This return value is useful for determining
     * whether the app needs to be recreated to apply the new language.
     *
     * ## Behavior
     * - Compares new value with current value before saving
     * - Only writes to preferences if the value actually changed (optimization)
     * - Returns `true` if the language changed, `false` if it was already set
     * - Does not apply the language (use [LocaleHelper.updateAppLanguage] for that)
     *
     * ## Return Value Usage
     * The boolean return value allows callers to conditionally trigger UI recreation:
     * ```kotlin
     * if (PreferenceHelper.setAppLanguage("es")) {
     *     activity.recreate() // Only recreate if language changed
     * }
     * ```
     *
     * ## Common Values
     * - **"sys"**: Use system default language
     * - **"en"**: English
     * - **"es"**: Spanish
     * - **"zh"**: Simplified Chinese
     * - **"zh-TW"**: Traditional Chinese
     * - ... other ISO 639-1 codes
     *
     * ## Usage Example
     * ```kotlin
     * // Basic usage
     * PreferenceHelper.setAppLanguage("fr")
     *
     * // With recreation logic
     * if (PreferenceHelper.setAppLanguage(selectedLanguage)) {
     *     // Language changed, update UI
     *     activity.recreate()
     * } else {
     *     // No change, no action needed
     *     showToast("Language already set to ${selectedLanguage}")
     * }
     * ```
     *
     * ## Important Notes
     * - This method only saves the preference; it doesn't apply the language
     * - Use [LocaleHelper.updateAppLanguage] for a complete language change operation
     * - The app must be recreated for the new language to take effect
     *
     * @param languageCode The language code to set. Use `"sys"` for system default, or
     * ISO 639-1 codes like "en", "es", or extended codes like "zh-TW".
     * @return `true` if the language preference changed, `false` if it was already set to
     * the specified value.
     *
     * @see getAppLanguage for retrieving the current language setting
     * @see LocaleHelper.updateAppLanguage for a higher-level API that handles both saving and applying
     */
    fun setAppLanguage(languageCode: String): Boolean {
        val current = getAppLanguage()
        if (current != languageCode) {
            putString(Preferences.APP_LANGUAGE, languageCode)
            return true
        }
        return false
    }
}