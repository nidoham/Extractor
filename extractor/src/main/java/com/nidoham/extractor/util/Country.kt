package com.nidoham.extractor.util

/**
 * Represents a country or language option for user selection and localization.
 *
 * This lightweight data class provides a simple abstraction for representing geographic regions
 * or languages in the application. It's commonly used in settings screens for language selection,
 * region/country selection, and content localization preferences.
 *
 * ## Use Cases
 * - Language selection dropdowns in settings
 * - Region/country selection for trending content
 * - Content localization preferences
 * - Display of available locale options
 *
 * ## Usage Example
 * ```kotlin
 * val country = Country(
 *     name = "United States",
 *     code = "US"
 * )
 *
 * // Display in UI
 * textView.text = country.name
 *
 * // Save preference
 * preferences.setRegion(country.code)
 * ```
 *
 * @property name The human-readable display name of the country or language.
 * This should be localized where appropriate (e.g., "United States", "Estados Unidos").
 * Used for display in UI components like dropdowns and lists.
 * @property code The ISO standard code for the country or language. For countries, this is
 * typically the ISO 3166-1 alpha-2 code (e.g., "US", "GB", "JP"). For languages, this is
 * the ISO 639-1 code (e.g., "en", "es", "zh") or extended codes like "zh-TW" for regional
 * variants. Used for programmatic operations and API calls.
 *
 * @see LocaleHelper.getAvailableCountries for retrieving a list of all available countries
 * @see LocaleHelper.getAvailableLocales for retrieving a list of all available languages
 */
data class Country(
    val name: String,
    val code: String
)