package com.nidoham.extractor.stream

import com.nidoham.extractor.util.Kiosk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskExtractor

class TrendsExtractor {

    enum class Category(val kioskId: String, val displayName: String) {
        TRENDING(Kiosk.TRENDING, Kiosk.TRENDING_DISPLAY_NAME),
        LIVE(Kiosk.LIVE, Kiosk.LIVE_DISPLAY_NAME),
        MUSIC(Kiosk.MUSIC, Kiosk.MUSIC_DISPLAY_NAME),
        GAMING(Kiosk.GAMING, Kiosk.GAMING_DISPLAY_NAME),
        MOVIES_AND_SHOWS(Kiosk.MOVIES_AND_SHOWS, Kiosk.MOVIES_AND_SHOWS_DISPLAY_NAME),
        PODCASTS_EPISODES(Kiosk.PODCASTS_EPISODES, Kiosk.PODCASTS_EPISODES_DISPLAY_NAME)
    }

    data class PageResult(
        val items: List<StreamItem>,
        val nextPage: Page?,
        val hasNextPage: Boolean
    )

    private val service = NewPipe.getService(ServiceList.YouTube.serviceId)

    /**
     * Fetch and return the first page of content for a given [category].
     *
     * @throws IllegalArgumentException if the category's kiosk ID is unavailable in this region/service.
     */
    suspend fun fetchInitialPage(category: Category): PageResult = withContext(Dispatchers.IO) {
        val extractor = getKioskExtractor(category)
        extractor.fetchPage()

        val initialPage = extractor.initialPage
        PageResult(
            items = extractItems(initialPage.items),
            nextPage = initialPage.nextPage,
            hasNextPage = initialPage.hasNextPage()
        )
    }

    /**
     * Fetch the next page of content.
     *
     * A fresh extractor is created and its initial page is fetched before [getPage] is called
     * because some NewPipe extractors must be in an initialised state before pagination works.
     *
     * @throws IllegalArgumentException if the category's kiosk ID is unavailable.
     */
    suspend fun fetchNextPage(category: Category, page: Page): PageResult = withContext(Dispatchers.IO) {
        val extractor = getKioskExtractor(category)
        // Initialise before paginating — required by certain extractors.
        extractor.fetchPage()

        val nextInfoPage = extractor.getPage(page)
        PageResult(
            items = extractItems(nextInfoPage.items),
            nextPage = nextInfoPage.nextPage,
            hasNextPage = nextInfoPage.hasNextPage()
        )
    }

    /**
     * Returns a [KioskExtractor] for the given [category].
     *
     * Only [Category.TRENDING] uses the service default extractor. Every other category is
     * looked up by its exact kiosk ID; if the ID is absent for the current region/service an
     * [IllegalArgumentException] is thrown so the ViewModel can surface a proper error instead
     * of silently showing the wrong content.
     *
     * @throws IllegalArgumentException if the requested kiosk ID is not available.
     */
    @Throws(IllegalArgumentException::class)
    private fun getKioskExtractor(category: Category): KioskExtractor<*> {
        val kioskList = service.kioskList

        if (category == Category.TRENDING) {
            return kioskList.defaultKioskExtractor
        }

        if (kioskList.availableKiosks.contains(category.kioskId)) {
            return kioskList.getExtractorById(category.kioskId, null)
        }

        throw IllegalArgumentException(
            "Kiosk '${category.kioskId}' is not available for this region/service. " +
                    "Available kiosks: ${kioskList.availableKiosks}"
        )
    }

    /**
     * Converts a raw list of [InfoItem]s into [StreamItem]s.
     *
     * All item types (video, channel, playlist) are passed through [StreamItem.from] so that
     * mixed-type kiosk pages are handled correctly rather than non-video items being silently dropped.
     */
    private fun extractItems(items: List<InfoItem>): List<StreamItem> =
        items.map { StreamItem.from(it) }
}