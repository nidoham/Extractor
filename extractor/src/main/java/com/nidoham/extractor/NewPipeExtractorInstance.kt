package com.nidoham.extractor

import android.content.Context
import android.util.Log
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.Locale

/**
 * Singleton manager for NewPipe Extractor
 * Handles initialization and provides access to YouTube service
 */
object NewPipeExtractorInstance {

    private const val TAG = "NewPipeExtractor"

    @Volatile
    private var initialized = false

    private var downloaderImpl: NewPipeDownloaderImpl? = null

    /**
     * YouTube streaming service instance
     */
    val youtubeService: StreamingService by lazy {
        ensureInitialized()
        NewPipe.getService(ServiceList.YouTube.serviceId)
    }

    /**
     * Initialize NewPipe with context for caching
     * Call this in Application.onCreate() or before first use
     */
    fun init(context: Context) {
        if (initialized) {
            Log.d(TAG, "NewPipe already initialized")
            return
        }

        synchronized(this) {
            if (initialized) return

            try {
                // Create downloader with cache
                val cacheDir = context.cacheDir
                downloaderImpl = NewPipeDownloaderImpl(
                    cacheDir = cacheDir,
                    cacheSize = 20 * 1024 * 1024 // 20 MB cache
                )

                // Initialize NewPipe
                NewPipe.init(downloaderImpl)

                // Set localization based on device settings
                val locale = Locale.getDefault()
                val localization = Localization(locale.language, locale.country)
                NewPipe.setPreferredLocalization(localization)

                // Set content country
                val contentCountry = ContentCountry(locale.country)
                NewPipe.setPreferredContentCountry(contentCountry)

                initialized = true
                Log.i(TAG, "NewPipe initialized successfully with locale: $locale")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize NewPipe", e)
                throw RuntimeException("Failed to initialize NewPipe extractor", e)
            }
        }
    }

    /**
     * Initialize without context (no caching)
     * Use this only if you can't provide a context
     */
    fun initWithoutCache() {
        if (initialized) {
            Log.d(TAG, "NewPipe already initialized")
            return
        }

        synchronized(this) {
            if (initialized) return

            try {
                downloaderImpl = NewPipeDownloaderImpl()
                NewPipe.init(downloaderImpl)

                initialized = true
                Log.i(TAG, "NewPipe initialized without cache")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize NewPipe", e)
                throw RuntimeException("Failed to initialize NewPipe extractor", e)
            }
        }
    }

    /**
     * Check if NewPipe is initialized
     */
    fun isInitialized(): Boolean = initialized

    /**
     * Ensure NewPipe is initialized before use
     */
    private fun ensureInitialized() {
        if (!initialized) {
            throw IllegalStateException(
                "NewPipe not initialized. Call NewPipeExtractorInstance.init(context) first."
            )
        }
    }

    /**
     * Update localization at runtime
     */
    fun updateLocalization(languageCode: String, countryCode: String) {
        ensureInitialized()

        val localization = Localization(languageCode, countryCode)
        NewPipe.setPreferredLocalization(localization)

        val contentCountry = ContentCountry(countryCode)
        NewPipe.setPreferredContentCountry(contentCountry)

        Log.i(TAG, "Localization updated to: $languageCode-$countryCode")
    }

    /**
     * Clean up resources
     * Call this when app is being destroyed
     */
    fun shutdown() {
        downloaderImpl?.shutdown()
        initialized = false
        Log.i(TAG, "NewPipe shutdown complete")
    }

    /**
     * Get the current localization
     */
    fun getCurrentLocalization(): Localization? {
        return if (initialized) {
            NewPipe.getPreferredLocalization()
        } else {
            null
        }
    }

    /**
     * Get the current content country
     */
    fun getCurrentContentCountry(): ContentCountry? {
        return if (initialized) {
            NewPipe.getPreferredContentCountry()
        } else {
            null
        }
    }
}