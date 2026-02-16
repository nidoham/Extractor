package com.nidoham.extractor.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/**
 * Centralized helper for managing application localization, language preferences, and regional settings.
 *
 * This singleton object provides a comprehensive suite of utilities for handling multi-language support,
 * runtime language switching, country detection, and locale management throughout the application. It serves
 * as the primary interface for all localization-related operations, ensuring consistent behavior across
 * different Android versions and device configurations.
 *
 * ## Key Features
 * - **Runtime Language Changes**: Switch app language without reinstalling
 * - **Automatic Country Detection**: Detect user's country from SIM, network, or locale
 * - **Content Localization**: Separate UI language from content language preferences
 * - **Locale Utilities**: Parse and create Locale objects from various code formats
 * - **Available Options**: Retrieve lists of supported languages and countries
 * - **Android Version Compatibility**: Handles differences between Android API levels
 *
 * ## Architecture
 * The helper distinguishes between two types of localization:
 * 1. **UI Language**: The language used for app interface text (buttons, menus, etc.)
 * 2. **Content Language**: The language preference for fetched content (videos, descriptions, etc.)
 *
 * ## Usage Example
 * ```kotlin
 * // In Application.onCreate()
 * class MyApp : Application() {
 *     override fun attachBaseContext(base: Context) {
 *         super.attachBaseContext(LocaleHelper.setLocale(base))
 *     }
 * }
 *
 * // In Activity
 * class MainActivity : AppCompatActivity() {
 *     override fun attachBaseContext(newBase: Context) {
 *         super.attachBaseContext(LocaleHelper.setLocale(newBase))
 *     }
 *
 *     // When user changes language
 *     fun onLanguageSelected(languageCode: String) {
 *         LocaleHelper.updateAppLanguage(this, languageCode)
 *     }
 * }
 *
 * // Detect user's country
 * val country = LocaleHelper.getDetectedCountry(context)
 *
 * // Get available languages for selection
 * val languages = LocaleHelper.getAvailableLocales()
 * ```
 *
 * ## Thread Safety
 * This object is thread-safe for read operations. Language change operations should be performed
 * on the main thread as they trigger UI recreation.
 *
 * @see PreferenceHelper for storage of language preferences
 * @see Country for representing language and country options
 */
object LocaleHelper {

    /* ========================================================================
       RUNTIME LANGUAGE CHANGE
       ======================================================================== */

    /**
     * Applies the saved language preference to the provided context.
     *
     * This method retrieves the user's language preference from SharedPreferences and creates
     * a new context with the appropriate locale configuration. It should be called in both
     * `Application.attachBaseContext()` and `Activity.attachBaseContext()` to ensure the
     * language is applied consistently throughout the app lifecycle.
     *
     * ## Behavior
     * - If the saved preference is `"sys"` (system default), returns the context unchanged
     * - Otherwise, creates a new context with the specified language configuration
     * - Handles differences between Android versions (pre-N and N+)
     * - Does not modify the original context, returns a new instance
     *
     * ## Android Version Support
     * - **Android N+ (API 24+)**: Uses `createConfigurationContext()` (recommended approach)
     * - **Pre-Android N**: Uses deprecated `updateConfiguration()` (legacy support)
     *
     * ## Integration Pattern
     * ```kotlin
     * override fun attachBaseContext(base: Context) {
     *     super.attachBaseContext(LocaleHelper.setLocale(base))
     * }
     * ```
     *
     * @param context The base context to apply the locale to. This should be the context
     * provided to `attachBaseContext()`.
     * @return A new context with the appropriate locale configuration applied, or the original
     * context if using system default language.
     *
     * @see updateAppLanguage for changing the language at runtime
     * @see PreferenceHelper.getAppLanguage for retrieving the saved preference
     */
    fun setLocale(context: Context): Context {
        val languageCode = PreferenceHelper.getAppLanguage()
        return if (languageCode == "sys") {
            context // Use system default
        } else {
            updateResources(context, languageCode)
        }
    }

    /**
     * Updates the app language at runtime and recreates the activity to apply changes.
     *
     * This method provides a seamless way to change the application language without requiring
     * the user to restart the app. It saves the new language preference and immediately recreates
     * the current activity to apply the language change throughout the UI.
     *
     * ## Behavior
     * - Saves the new language code to SharedPreferences
     * - Only recreates the activity if the language actually changed (optimization)
     * - The recreation triggers `attachBaseContext()` which applies the new locale
     * - Activity state is preserved during recreation (via saved instance state)
     *
     * ## Important Notes
     * - This method must be called on the main thread (it triggers UI operations)
     * - The activity will be destroyed and recreated, so ensure proper state preservation
     * - All other activities in the stack will also apply the new language when resumed
     * - For `"sys"` language code, the app will use the device's system language
     *
     * ## Usage Example
     * ```kotlin
     * // In settings screen when user selects a language
     * languageSpinner.setOnItemSelectedListener { position ->
     *     val selectedLanguage = languages[position]
     *     LocaleHelper.updateAppLanguage(this, selectedLanguage.code)
     *     // Activity will be automatically recreated with new language
     * }
     * ```
     *
     * @param activity The activity to recreate after changing the language. This is typically
     * the settings activity where the language change was initiated.
     * @param languageCode The new language code to apply. Use `"sys"` for system default,
     * or ISO 639-1 codes like `"en"`, `"es"`, `"zh"`, or extended codes like `"zh-TW"`.
     *
     * @see setLocale for applying the language during app initialization
     * @see getAvailableLocales for retrieving valid language codes
     */
    fun updateAppLanguage(activity: Activity, languageCode: String) {
        if (PreferenceHelper.setAppLanguage(languageCode)) {
            // Language changed, recreate activity to apply new locale
            activity.recreate()
        }
    }

    /**
     * Updates context resources with the specified language configuration.
     *
     * This internal method handles the platform-specific logic for applying a locale to a context.
     * It creates a new configuration with the desired locale and returns a context that uses it.
     * The implementation differs between Android N+ and earlier versions due to API changes.
     *
     * ## Implementation Details
     * - Parses the language code into a Locale object
     * - Sets the Locale as the JVM default (affects SimpleDateFormat, etc.)
     * - Creates a new Configuration with the locale
     * - Returns a context using the new configuration
     *
     * ## Android Version Handling
     * - **API 24+ (Android N)**: Uses `createConfigurationContext()` which is the recommended
     *   approach and properly handles multi-locale configurations
     * - **API < 24**: Uses deprecated `updateConfiguration()` which modifies resources in place
     *
     * @param context The original context to base the new context on.
     * @param languageCode The language code to apply. Can be simple codes like `"en"` or
     * extended codes like `"zh-CN"` or `"zh-rCN"`.
     * @return A new context with the specified locale configuration applied.
     *
     * @see getLocaleFromAndroidCode for parsing language codes into Locale objects
     */
    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = getLocaleFromAndroidCode(languageCode)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }

    /* ========================================================================
       LOCALE UTILITIES
       ======================================================================== */

    /**
     * Retrieves the app's locale for content preferences (not UI language).
     *
     * This method returns the locale used for determining which language content to fetch
     * from external services (e.g., YouTube video titles, descriptions). This is separate
     * from the UI language preference and allows users to have an English UI while viewing
     * content in Spanish, for example.
     *
     * ## Behavior
     * - If the preference is set to `"sys"`, returns the device's default locale
     * - Otherwise, returns a locale created from the saved language code
     * - The returned locale can be used for API calls and content filtering
     *
     * ## Use Cases
     * - Determining which language of content to request from YouTube
     * - Setting the locale for NewPipe extractor operations
     * - Filtering search results by language
     *
     * ## Usage Example
     * ```kotlin
     * val contentLocale = LocaleHelper.getAppLocale()
     * val localization = Localization(contentLocale.language, contentLocale.country)
     * NewPipe.setPreferredLocalization(localization)
     * ```
     *
     * @return The Locale object representing the user's content language preference.
     * Never returns null - defaults to system locale if preference not set.
     *
     * @see PreferenceHelper.getString for preference retrieval
     * @see getLocaleFromAndroidCode for parsing the language code
     */
    fun getAppLocale(): Locale {
        val languageName = PreferenceHelper.getString(Preferences.CONTENT_LANGUAGE, "sys")
        return if (languageName == "sys") {
            Locale.getDefault()
        } else {
            getLocaleFromAndroidCode(languageName)
        }
    }

    /**
     * Converts a language code string into a Locale object.
     *
     * This utility method handles various language code formats and normalizes them into proper
     * Java Locale objects. It supports both simple language codes and region-specific variants,
     * handling different formatting conventions.
     *
     * ## Supported Formats
     * - **Simple codes**: `"en"`, `"es"`, `"zh"` → Locale with language only
     * - **Language-Country**: `"zh-CN"`, `"en-US"` → Locale with language and country
     * - **Android format**: `"zh-rCN"`, `"pt-rBR"` → Normalized to `"zh-CN"`, `"pt-BR"`
     *
     * ## Processing Rules
     * - Hyphens with 'r' prefix (`-r`) are normalized to plain hyphens (`-`)
     * - Country codes are converted to uppercase for consistency
     * - Invalid or malformed codes are handled gracefully (no exceptions thrown)
     *
     * ## Examples
     * ```kotlin
     * getLocaleFromAndroidCode("en")        // → Locale("en")
     * getLocaleFromAndroidCode("zh-CN")     // → Locale("zh", "CN")
     * getLocaleFromAndroidCode("zh-rCN")    // → Locale("zh", "CN")
     * getLocaleFromAndroidCode("pt-rBR")    // → Locale("pt", "BR")
     * ```
     *
     * @param code The language code string to parse. Can include country/region codes
     * separated by hyphen, with or without the Android 'r' prefix.
     * @return A Locale object representing the parsed code. For simple codes, only the
     * language is set. For compound codes, both language and country are set.
     *
     * @see getAppLocale for retrieving the app's content locale preference
     * @see updateResources for applying locales to contexts
     */
    fun getLocaleFromAndroidCode(code: String): Locale {
        val normalizedCode = code.replace("-r", "-")
        return if (normalizedCode.contains("-")) {
            val parts = normalizedCode.split("-", limit = 2)
            Locale(parts[0], parts[1].uppercase())
        } else {
            Locale(normalizedCode)
        }
    }

    /* ========================================================================
       COUNTRY/REGION DETECTION
       ======================================================================== */

    /**
     * Detects the user's country using multiple fallback methods.
     *
     * This method attempts to determine the user's country through a hierarchy of detection
     * methods, falling back to less accurate methods if better ones are unavailable. It's
     * particularly useful for initializing region-specific content (e.g., YouTube trending videos)
     * when the user hasn't explicitly set a preference.
     *
     * ## Detection Hierarchy (in order of preference)
     * 1. **SIM Card Country**: Most reliable for mobile devices with active SIM
     * 2. **Network Country**: Fallback for devices without SIM or when SIM country unavailable
     * 3. **Locale Country**: Based on device language/region settings
     * 4. **Default**: Returns `"UK"` if all detection methods fail
     *
     * ## Method Characteristics
     * - **SIM Detection**: Accurate for local SIM cards, may be incorrect for travelers
     * - **Network Detection**: Reflects current network location, good for roaming users
     * - **Locale Detection**: Based on device settings, may not reflect physical location
     *
     * ## Use Cases
     * - Setting initial trending region preference
     * - Determining default content country for YouTube
     * - Initializing region-specific features on first launch
     *
     * ## Important Notes
     * - Requires `READ_PHONE_STATE` permission for SIM detection (gracefully degrades if denied)
     * - All country codes are returned in uppercase (ISO 3166-1 alpha-2 format)
     * - The default `"UK"` is used as a neutral fallback region
     *
     * ## Usage Example
     * ```kotlin
     * // On first launch, initialize trending region
     * if (isFirstLaunch) {
     *     val detectedCountry = LocaleHelper.getDetectedCountry(context)
     *     preferences.setRegion(detectedCountry)
     * }
     * ```
     *
     * @param context The application or activity context. Used to access system services
     * like TelephonyManager and to read device configuration.
     * @return The detected country code in uppercase (e.g., `"US"`, `"GB"`, `"JP"`), or
     * `"UK"` if detection fails. Never returns null.
     *
     * @see detectSIMCountry for SIM-based detection details
     * @see detectNetworkCountry for network-based detection details
     * @see detectLocaleCountry for locale-based detection details
     */
    fun getDetectedCountry(context: Context): String {
        return detectSIMCountry(context)
            ?: detectNetworkCountry(context)
            ?: detectLocaleCountry(context)
            ?: "UK"
    }

    /**
     * Detects the country code from the device's SIM card.
     *
     * This internal method attempts to read the ISO country code from the device's SIM card,
     * which is typically the most reliable indicator of the user's home country on mobile devices.
     *
     * ## Behavior
     * - Requires a SIM card to be present and active
     * - Returns the ISO 3166-1 alpha-2 country code from the SIM
     * - Converts the code to uppercase for consistency
     * - Returns null if no SIM, if the SIM country is unavailable, or if permission is denied
     *
     * ## Permissions
     * - **Android 6.0+**: May require `READ_PHONE_STATE` permission (handled gracefully)
     * - Permission denial results in null return (not an exception)
     *
     * ## Limitations
     * - Not available on tablets or devices without cellular capability
     * - May be incorrect for travelers using home country SIM abroad
     * - Some MVNOs may report unexpected country codes
     *
     * @param context The context used to access the TelephonyManager system service.
     * @return The uppercase country code from the SIM (e.g., `"US"`, `"GB"`), or null if
     * unavailable, empty, or if permission is denied.
     */
    private fun detectSIMCountry(context: Context): String? {
        return context.getSystemService<TelephonyManager>()
            ?.simCountryIso
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase()
    }

    /**
     * Detects the country code from the current cellular network.
     *
     * This internal method attempts to determine the country based on the cellular network
     * the device is currently connected to. This is particularly useful for detecting the
     * user's current location when roaming internationally.
     *
     * ## Behavior
     * - Requires an active cellular network connection
     * - Returns the ISO 3166-1 alpha-2 country code of the network operator
     * - Converts the code to uppercase for consistency
     * - Returns null if no cellular connection, if network country is unavailable, or if permission is denied
     *
     * ## Permissions
     * - **Android 6.0+**: May require `READ_PHONE_STATE` permission (handled gracefully)
     * - Permission denial results in null return (not an exception)
     *
     * ## Use Cases
     * - Better than SIM detection for international travelers
     * - Reflects current physical location more accurately when roaming
     * - Useful when SIM country is unavailable
     *
     * ## Limitations
     * - Not available on Wi-Fi-only devices
     * - Requires active cellular connection (fails in airplane mode)
     * - May not be available in areas with poor cellular coverage
     *
     * @param context The context used to access the TelephonyManager system service.
     * @return The uppercase country code from the network (e.g., `"US"`, `"JP"`), or null if
     * unavailable, empty, or if permission is denied.
     */
    private fun detectNetworkCountry(context: Context): String? {
        return context.getSystemService<TelephonyManager>()
            ?.networkCountryIso
            ?.takeIf { it.isNotEmpty() }
            ?.uppercase()
    }

    /**
     * Detects the country code from the device's locale configuration.
     *
     * This internal method retrieves the country code from the device's configured locale,
     * which is typically set during device setup or in the system settings. This is the most
     * broadly available detection method but may not reflect the user's physical location.
     *
     * ## Behavior
     * - Reads from the system's configured locale list
     * - Uses the first (primary) locale in the list
     * - Extracts the country component of the locale
     * - Returns null if the country component is not set or is empty
     *
     * ## Android Version Support
     * - Uses `ConfigurationCompat` for compatibility across all Android versions
     * - Properly handles locale lists on Android N+ and single locale on older versions
     *
     * ## Characteristics
     * - **Always Available**: Works on all devices regardless of SIM or network
     * - **User Controlled**: Reflects the user's explicit region/language settings
     * - **Location Independent**: May not match physical location (e.g., expat settings)
     *
     * ## Limitations
     * - May not be set on some devices or in some locales
     * - User may have selected a country different from their physical location
     * - Some language-only locales (e.g., "en") may not include a country
     *
     * @param context The context used to access the configuration and locale information.
     * @return The country code from the locale (e.g., `"US"`, `"GB"`), or null if the locale
     * doesn't include a country component or if it's empty.
     */
    private fun detectLocaleCountry(context: Context): String? {
        return ConfigurationCompat.getLocales(context.resources.configuration)
            .get(0)
            ?.country
            ?.takeIf { it.isNotEmpty() }
    }

    /* ========================================================================
       AVAILABLE LANGUAGES & REGIONS
       ======================================================================== */

    /**
     * Retrieves a comprehensive list of all available countries/regions for user selection.
     *
     * This method generates a complete list of countries based on the ISO 3166-1 standard,
     * providing localized country names and their corresponding codes. The list is suitable
     * for displaying in settings or selection dialogs.
     *
     * ## Data Source
     * - Uses `Locale.getISOCountries()` which provides all ISO 3166-1 alpha-2 country codes
     * - Country names are localized using the device's current locale
     * - Results are sorted alphabetically by country name for easy browsing
     *
     * ## Return Format
     * Each country is represented as a [Country] object with:
     * - **name**: Localized country name (e.g., "United States", "Japan", "Brazil")
     * - **code**: Uppercase ISO country code (e.g., "US", "JP", "BR")
     *
     * ## Characteristics
     * - **Complete Coverage**: Includes all 249 ISO-defined countries
     * - **Localized**: Country names match the device language
     * - **Sorted**: Alphabetically ordered for user convenience
     * - **Uppercase Codes**: Consistent formatting for comparison and storage
     *
     * ## Usage Example
     * ```kotlin
     * val countries = LocaleHelper.getAvailableCountries()
     *
     * // Display in a spinner or list
     * countrySpinner.adapter = ArrayAdapter(
     *     context,
     *     android.R.layout.simple_spinner_item,
     *     countries.map { it.name }
     * )
     *
     * // When user selects a country
     * countrySpinner.setOnItemSelectedListener { position ->
     *     val selectedCountry = countries[position]
     *     preferences.setRegion(selectedCountry.code)
     * }
     * ```
     *
     * @return A sorted list of [Country] objects representing all available countries.
     * The list is always non-null and contains 200+ entries. Country names are localized
     * to the device's current locale.
     *
     * @see Country for the data structure
     * @see getAvailableLocales for language selection instead of countries
     */
    fun getAvailableCountries(): List<Country> {
        return Locale.getISOCountries()
            .map { countryCode ->
                Country(
                    name = Locale("", countryCode).displayCountry,
                    code = countryCode.uppercase()
                )
            }
            .sortedBy { it.name }
    }

    /**
     * Retrieves a curated list of commonly used languages for user selection.
     *
     * This method returns a predefined list of the most commonly used languages for better UX,
     * rather than generating from all available locales (which would include hundreds of options).
     * The list includes major world languages and is suitable for language selection in settings.
     *
     * ## Language Coverage
     * The list includes:
     * - **Major International Languages**: English, Spanish, French, German, Chinese, etc.
     * - **Regional Variants**: Simplified Chinese, Traditional Chinese, etc.
     * - **South Asian Languages**: Hindi, Bengali, Tamil, Telugu, Urdu, etc.
     * - **Southeast Asian Languages**: Indonesian, Thai, Vietnamese, Filipino, etc.
     * - **Middle Eastern Languages**: Arabic, Persian, Hebrew, Turkish, etc.
     * - **European Languages**: All major European languages plus regional variants
     *
     * ## Design Rationale
     * - **Curated vs Complete**: Provides ~40 languages instead of 200+ for better usability
     * - **User Experience**: Common languages are easier to find in a shorter list
     * - **Maintainability**: Can be easily updated to add new popular languages
     * - **Sorted**: Alphabetically ordered for easy navigation
     *
     * ## Return Format
     * Each language is represented as a [Country] object (reused for simplicity) with:
     * - **name**: English display name (e.g., "English", "Spanish", "Chinese (Simplified)")
     * - **code**: ISO 639-1 or extended code (e.g., "en", "es", "zh", "zh-TW")
     *
     * ## Usage Example
     * ```kotlin
     * val languages = LocaleHelper.getAvailableLocales()
     *
     * // Display in settings
     * languageSpinner.adapter = ArrayAdapter(
     *     context,
     *     android.R.layout.simple_spinner_item,
     *     languages.map { it.name }
     * )
     *
     * // When user selects a language
     * languageSpinner.setOnItemSelectedListener { position ->
     *     val selectedLanguage = languages[position]
     *     LocaleHelper.updateAppLanguage(this, selectedLanguage.code)
     * }
     * ```
     *
     * ## Extending the List
     * To add more languages, simply add entries to the returned list with appropriate
     * language names and ISO codes. Maintain alphabetical sorting for consistency.
     *
     * @return A sorted list of [Country] objects representing curated, commonly-used languages.
     * The list is always non-null and contains ~40 languages. Language names are in English
     * to maintain consistency across all device languages.
     *
     * @see Country for the data structure (reused for languages)
     * @see getAvailableCountries for country selection instead of languages
     * @see updateAppLanguage for applying a selected language
     */
    fun getAvailableLocales(): List<Country> {
        // Predefined list of common languages for better UX
        return listOf(
            Country("English", "en"),
            Country("Spanish", "es"),
            Country("French", "fr"),
            Country("German", "de"),
            Country("Italian", "it"),
            Country("Portuguese", "pt"),
            Country("Russian", "ru"),
            Country("Japanese", "ja"),
            Country("Korean", "ko"),
            Country("Chinese (Simplified)", "zh"),
            Country("Chinese (Traditional)", "zh-TW"),
            Country("Arabic", "ar"),
            Country("Hindi", "hi"),
            Country("Turkish", "tr"),
            Country("Dutch", "nl"),
            Country("Polish", "pl"),
            Country("Vietnamese", "vi"),
            Country("Indonesian", "id"),
            Country("Thai", "th"),
            Country("Swedish", "sv"),
            Country("Norwegian", "no"),
            Country("Danish", "da"),
            Country("Finnish", "fi"),
            Country("Greek", "el"),
            Country("Hebrew", "he"),
            Country("Czech", "cs"),
            Country("Hungarian", "hu"),
            Country("Romanian", "ro"),
            Country("Ukrainian", "uk"),
            Country("Bengali", "bn"),
            Country("Malay", "ms"),
            Country("Persian", "fa"),
            Country("Urdu", "ur"),
            Country("Tamil", "ta"),
            Country("Telugu", "te"),
            Country("Marathi", "mr"),
            Country("Gujarati", "gu"),
            Country("Kannada", "kn"),
            Country("Filipino", "fil"),
            Country("Swahili", "sw"),
            Country("Amharic", "am")
        ).sortedBy { it.name }
    }

    /**
     * Retrieves the current system language code from the device configuration.
     *
     * This method extracts the primary language code from the system's locale configuration,
     * providing the language the device UI is currently using. This is useful for detecting
     * the device language independently of app preferences.
     *
     * ## Behavior
     * - Returns only the language component (e.g., "en" from "en_US")
     * - Uses the primary (first) locale on Android N+ devices with multiple locales
     * - Handles both modern and legacy Android versions transparently
     *
     * ## Android Version Support
     * - **API 24+ (Android N)**: Uses locale list, returns first locale's language
     * - **API < 24**: Uses deprecated single locale configuration
     *
     * ## Use Cases
     * - Detecting device language on first launch
     * - Comparing app language to system language
     * - Providing "use system language" functionality
     * - Analytics and debugging
     *
     * ## Usage Example
     * ```kotlin
     * val systemLang = LocaleHelper.getSystemLanguage(context)
     *
     * // Show language mismatch notification
     * if (systemLang != currentAppLanguage) {
     *     showNotification("App language differs from system")
     * }
     *
     * // Offer to match system language
     * if (isFirstLaunch) {
     *     askToUseSystemLanguage(systemLang)
     * }
     * ```
     *
     * @param context The context used to access the configuration and locale information.
     * @return The ISO 639-1 language code of the system's primary locale (e.g., "en", "es",
     * "zh"). Never returns null - will return a valid language code even if it's a default.
     *
     * @see getAppLocale for the app's content language preference
     */
    fun getSystemLanguage(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }
}