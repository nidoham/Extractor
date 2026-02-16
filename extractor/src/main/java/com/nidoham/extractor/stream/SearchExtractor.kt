package com.nidoham.extractor.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.search.SearchExtractor as NPSearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Coroutine-safe wrapper for NewPipe search functionality with pagination and filtering support.
 *
 * This class provides a robust, Kotlin-idiomatic interface for searching YouTube content through NewPipe,
 * with support for filtering by content type, sorting results, pagination, and autocomplete suggestions.
 * All operations are executed asynchronously using Kotlin coroutines with automatic retry logic for
 * improved reliability.
 *
 * ## Key Features
 * - **Content Type Filtering**: Search specifically for videos, channels, playlists, songs, or albums
 * - **Sort Options**: Results can be sorted by relevance, upload date, view count, or rating
 * - **Pagination Support**: Efficient pagination through large result sets using session-based continuation
 * - **Autocomplete Suggestions**: Real-time search suggestions as the user types
 * - **Search Correction**: Automatic detection of corrected searches (e.g., "did you mean...")
 * - **Automatic Retry**: Failed requests are automatically retried with exponential backoff
 * - **Mixed Content**: Results can contain videos, channels, and playlists in a unified format
 *
 * ## Search Flow
 * 1. **Initial Search**: Call [search] with query and filters, returns first page + session
 * 2. **Pagination**: Use the session to call [fetchMore] for subsequent pages
 * 3. **Suggestions**: Optionally call [suggestions] for autocomplete during typing
 *
 * ## Usage Example
 * ```kotlin
 * val extractor = SearchExtractor(
 *     serviceId = ServiceList.YouTube.serviceId,
 *     config = SearchConfig(maxRetries = 5)
 * )
 *
 * // Perform initial search
 * val (result, session) = extractor.search(
 *     query = "kotlin tutorials",
 *     filter = SearchFilter.VIDEOS,
 *     sort = SortFilter.VIEWS
 * ).getOrThrow()
 *
 * println("Found ${result.items.size} results")
 * result.items.forEach { item ->
 *     println("${item.name} - ${item.uploaderName}")
 * }
 *
 * // Load more results if available
 * if (result.hasNextPage) {
 *     val moreResults = extractor.fetchMore(session).getOrThrow()
 *     println("Loaded ${moreResults.items.size} more results")
 * }
 *
 * // Get search suggestions
 * val suggestions = extractor.suggestions("kotlin tu").getOrThrow()
 * println("Suggestions: $suggestions")
 * ```
 *
 * ## Thread Safety
 * This class is thread-safe. All search operations automatically execute on the IO dispatcher,
 * making it safe to call from any coroutine context including the main thread. However, note that
 * [SearchSession] objects maintain internal state and should not be shared across concurrent operations.
 *
 * @property serviceId The NewPipe service identifier for the platform to search. Defaults to YouTube.
 * Use [ServiceList] constants to specify different platforms if supported.
 * @property config Configuration for retry behavior and error recovery. Defaults to standard settings.
 *
 * @see SearchConfig for configuration options
 * @see SearchFilter for available content type filters
 * @see SortFilter for available sorting options
 * @see SearchResult for the structure of search results
 * @see SearchSession for pagination state management
 */
class SearchExtractor(
    private val serviceId: Int = ServiceList.YouTube.serviceId,
    private val config: SearchConfig = SearchConfig()
) {

    /**
     * Configuration parameters for search operations and retry behavior.
     *
     * This data class encapsulates the configurable aspects of search operations, particularly
     * error recovery strategies. These settings allow fine-tuning of the search process to balance
     * between reliability and responsiveness.
     *
     * ## Usage Example
     * ```kotlin
     * val config = SearchConfig(
     *     maxRetries = 5,           // Retry failed requests up to 5 times
     *     retryDelayMillis = 1000L  // Wait 1 second between retries
     * )
     * val extractor = SearchExtractor(config = config)
     * ```
     *
     * @property maxRetries The maximum number of retry attempts for failed network requests before giving up.
     * Each retry applies an exponentially increasing delay. Default is `3` retries, allowing up to 4 total
     * attempts (initial attempt + 3 retries).
     * @property retryDelayMillis The base delay in milliseconds between retry attempts. The actual delay
     * increases exponentially with each retry: `retryDelayMillis * (attempt + 1)`. Default is `500L`
     * (500 milliseconds), making retries relatively quick for search operations.
     */
    data class SearchConfig(
        val maxRetries: Int = 3,
        val retryDelayMillis: Long = 500L
    )

    /**
     * Content type filters for narrowing search results to specific categories.
     *
     * These filters correspond to YouTube's search filters and allow users to search for specific
     * types of content. Using a filter improves result relevance and reduces the amount of data
     * processed when users know what type of content they're looking for.
     *
     * ## Filter Behavior
     * - **ALL**: Returns mixed results including videos, channels, playlists, etc.
     * - **Specific Filters**: Return only items of the specified type
     * - Multiple filters cannot be combined (YouTube API limitation)
     *
     * @property value The internal filter value passed to NewPipe's search API. These correspond to
     * YouTube's content filter identifiers.
     */
    enum class SearchFilter(val value: String) {
        /**
         * Returns all types of content without filtering.
         *
         * This is the default filter and will return a mix of videos, channels, playlists,
         * and other content types in the results.
         */
        ALL("all"),

        /**
         * Returns only video content.
         *
         * Filters results to show only standard YouTube videos and shorts, excluding
         * channels, playlists, and other content types.
         */
        VIDEOS("videos"),

        /**
         * Returns only channel/creator profiles.
         *
         * Filters results to show only YouTube channels, useful for finding content creators
         * rather than individual videos.
         */
        CHANNELS("channels"),

        /**
         * Returns only playlist collections.
         *
         * Filters results to show only playlists, which are curated collections of videos
         * created by users or channels.
         */
        PLAYLISTS("playlists"),

        /**
         * Returns only individual music tracks/songs.
         *
         * Filters results to show music content categorized as individual songs,
         * typically from YouTube Music.
         */
        SONGS("music_songs"),

        /**
         * Returns only music albums.
         *
         * Filters results to show music content categorized as albums (collections of songs),
         * typically from YouTube Music.
         */
        ALBUMS("music_albums")
    }

    /**
     * Sort options for ordering search results.
     *
     * These sort filters determine the order in which search results are presented. Different
     * sort orders are useful for different search scenarios - finding the most relevant content,
     * discovering recent uploads, or finding the most popular videos.
     *
     * ## Sort Behavior
     * - Sorting is applied after filtering and affects the entire result set
     * - The default sort is [RELEVANCE] which uses YouTube's ranking algorithm
     * - Sort order is maintained across pagination calls
     *
     * @property value The internal sort value passed to NewPipe's search API. These correspond to
     * YouTube's sort filter identifiers.
     */
    enum class SortFilter(val value: String) {
        /**
         * Sort by relevance to the search query.
         *
         * This is the default sort order and uses YouTube's algorithm to rank results based on
         * query matching, popularity, user engagement, and other relevance signals.
         */
        RELEVANCE("relevance"),

        /**
         * Sort by upload date, newest first.
         *
         * Orders results with the most recently uploaded content first. Useful for finding
         * the latest videos on a topic or from a creator.
         */
        DATE("upload_date"),

        /**
         * Sort by view count, highest first.
         *
         * Orders results by popularity based on total view count. Useful for finding the most
         * popular or viral content related to a query.
         */
        VIEWS("view_count"),

        /**
         * Sort by rating/engagement, highest first.
         *
         * Orders results by user rating and engagement metrics (likes, comments, etc.). Useful
         * for finding highly-rated content that viewers have actively engaged with.
         */
        RATING("rating")
    }

    /**
     * Search result container with items and pagination metadata.
     *
     * This data class encapsulates a page of search results along with metadata about the search
     * operation, pagination state, and any search corrections or suggestions provided by YouTube.
     *
     * ## Usage Example
     * ```kotlin
     * val result: SearchResult = extractor.search("kotlin").getOrThrow().first
     *
     * // Display results
     * result.items.forEach { item ->
     *     when (item.type) {
     *         StreamItem.ItemType.VIDEO -> displayVideo(item)
     *         StreamItem.ItemType.CHANNEL -> displayChannel(item)
     *         StreamItem.ItemType.PLAYLIST -> displayPlaylist(item)
     *         else -> displayUnknown(item)
     *     }
     * }
     *
     * // Check for search corrections
     * if (result.isCorrected && result.suggestion != null) {
     *     showMessage("Showing results for: ${result.suggestion}")
     * }
     *
     * // Enable load more button if needed
     * loadMoreButton.isEnabled = result.hasNextPage
     * ```
     *
     * @property items The list of search results for this page. These are unified [StreamItem] instances
     * that can represent videos, channels, playlists, or other content types. Items that fail to convert
     * are silently filtered out.
     * @property nextPage Pagination token for loading the next page of results. Returns `null` if this is
     * the last page. Pass this to [fetchMore] via the [SearchSession] to load additional results.
     * @property hasNextPage Indicates whether more results are available after this page. Equivalent to
     * `nextPage != null`, provided for convenience. When `false`, this is the final page of results.
     * @property suggestion Search suggestion text provided by YouTube, typically shown as "Did you mean: ...".
     * Returns `null` if no suggestion is available. This is different from autocomplete suggestions.
     * @property isCorrected Indicates whether YouTube automatically corrected the search query. When `true`,
     * the results shown are for a corrected version of the query rather than the exact query entered.
     * The corrected query is available in [suggestion].
     * @property query The original search query string that was used for this search. This may differ from
     * the query actually searched if [isCorrected] is `true`.
     *
     * @see StreamItem for the structure of individual result items
     * @see SearchSession for managing pagination across multiple result pages
     */
    data class SearchResult(
        val items: List<StreamItem>,
        val nextPage: Page?,
        val hasNextPage: Boolean,
        val suggestion: String?,
        val isCorrected: Boolean,
        val query: String
    )

    /**
     * Search session holder for managing pagination state.
     *
     * This data class maintains the state necessary to continue paginating through search results.
     * It contains a reference to the underlying NewPipe extractor and the pagination token needed
     * to fetch subsequent pages. Each search operation creates a new session that should be retained
     * for the duration of pagination through that search's results.
     *
     * ## Important Notes
     * - Sessions are **not thread-safe** and should not be shared across concurrent pagination operations
     * - Sessions maintain internal state in the NewPipe extractor and cannot be serialized
     * - A new session is required for each new search query or when filters/sort options change
     * - Sessions should be discarded when pagination is complete or when starting a new search
     *
     * ## Usage Pattern
     * ```kotlin
     * // Initial search creates session
     * val (initialResult, session) = extractor.search("kotlin").getOrThrow()
     *
     * // Use same session for all pagination
     * var currentSession = session
     * while (currentSession.nextPage != null) {
     *     val nextResult = extractor.fetchMore(currentSession).getOrThrow()
     *     // Update session for next iteration
     *     currentSession = currentSession.copy(nextPage = nextResult.nextPage)
     * }
     * ```
     *
     * @property extractor The internal NewPipe search extractor instance. This maintains the connection
     * to the search operation and should not be accessed directly. Marked as `internal` to prevent misuse.
     * @property query The search query string associated with this session. Retained for reference and
     * debugging purposes.
     * @property nextPage The current pagination token for fetching the next page of results. Returns `null`
     * when no more pages are available. This token must be passed to [fetchMore] to continue pagination.
     * @property filter The content type filter applied to this search. Retained for reference and to ensure
     * consistency across pagination calls.
     * @property sort The sort order applied to this search. Retained for reference and to ensure consistency
     * across pagination calls.
     *
     * @see SearchResult for the structure of results returned by this session
     * @see fetchMore for using the session to load additional pages
     */
    data class SearchSession(
        internal val extractor: NPSearchExtractor,
        val query: String,
        val nextPage: Page?,
        val filter: SearchFilter,
        val sort: SortFilter
    )

    /**
     * Performs an initial search and returns the first page of results with a pagination session.
     *
     * This method executes a search query against YouTube (or the configured service) and returns both
     * the first page of results and a session object for paginating through additional results. It applies
     * the specified content filter and sort order, and automatically handles search corrections and suggestions.
     *
     * The method executes on the IO dispatcher and blocks the calling coroutine until completion. Failed
     * requests are automatically retried according to the configured retry policy with exponential backoff.
     *
     * ## Search Behavior
     * - Minimum query length: No minimum enforced (YouTube handles empty/short queries)
     * - Maximum results per page: Determined by YouTube's API (typically 20-30 items)
     * - Result mixing: When using [SearchFilter.ALL], results may contain videos, channels, and playlists
     * - Search corrections: YouTube may automatically correct typos and return results for the corrected query
     *
     * ## Return Value
     * Returns a [Pair] containing:
     * 1. [SearchResult]: The first page of search results with metadata
     * 2. [SearchSession]: Session object for paginating through additional results
     *
     * Both components are required for full search functionality - the result for displaying items,
     * and the session for loading more pages.
     *
     * ## Error Handling
     * Returns `Result.failure` if:
     * - Network errors occur and all retry attempts are exhausted
     * - The search service is unavailable
     * - NewPipe extraction fails for any reason
     * - Invalid filter or sort combinations are provided
     *
     * ## Usage Example
     * ```kotlin
     * val (result, session) = extractor.search(
     *     query = "android development",
     *     filter = SearchFilter.VIDEOS,
     *     sort = SortFilter.DATE
     * ).getOrThrow()
     *
     * // Display first page
     * displayResults(result.items)
     *
     * // Store session for pagination
     * this.currentSession = session
     * ```
     *
     * @param query The search query string. Can include any characters and special search operators
     * supported by YouTube (e.g., "kotlin tutorial -beginner" or "site:youtube.com kotlin").
     * @param filter The content type filter to apply. Defaults to [SearchFilter.ALL] which returns
     * mixed content types. Use specific filters to narrow results to videos, channels, or playlists.
     * @param sort The sort order for results. Defaults to [SearchFilter.RELEVANCE] which uses YouTube's
     * ranking algorithm. Other options allow sorting by date, views, or rating.
     * @return A [Result] containing a [Pair] of [SearchResult] and [SearchSession] on success, or an
     * exception on failure. Use `getOrThrow()`, `getOrNull()`, or `fold()` to handle the result.
     *
     * @see fetchMore for loading additional pages using the session
     * @see SearchResult for the structure of returned results
     * @see SearchSession for managing pagination state
     * @see SearchFilter for available content filters
     * @see SortFilter for available sort options
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL,
        sort: SortFilter = SortFilter.RELEVANCE
    ): Result<Pair<SearchResult, SearchSession>> = withContext(Dispatchers.IO) {
        runWithRetry {
            val service = NewPipe.getService(serviceId)

            // Build content filter list (empty for ALL)
            val contentFilter = if (filter == SearchFilter.ALL) {
                emptyList()
            } else {
                listOf(filter.value)
            }

            // Create search query handler with filters
            val searchQueryHandler = service.searchQHFactory.fromQuery(
                query,
                contentFilter,
                sort.value
            )

            // Get and initialize the search extractor
            val extractor = service.getSearchExtractor(searchQueryHandler)
            extractor.fetchPage()

            // Extract first page of results
            val initialPage = extractor.initialPage

            val result = SearchResult(
                items = mapItems(initialPage.items),
                nextPage = initialPage.nextPage,
                hasNextPage = initialPage.hasNextPage(),
                suggestion = extractor.searchSuggestion.takeIf { it.isNotBlank() },
                isCorrected = extractor.isCorrectedSearch,
                query = query
            )

            // Create session for pagination
            val session = SearchSession(
                extractor = extractor,
                query = query,
                nextPage = initialPage.nextPage,
                filter = filter,
                sort = sort
            )

            result to session
        }
    }

    /**
     * Fetches the next page of search results using an active search session.
     *
     * This method continues pagination through search results by using the session created from
     * an initial [search] call. It retrieves the next page of results while maintaining the same
     * filter and sort settings from the original search.
     *
     * The method executes on the IO dispatcher and applies the same retry logic as the initial search.
     * Note that unlike the initial search, subsequent pages do not include search suggestions or
     * correction information (these are only available on the first page).
     *
     * ## Pagination Behavior
     * - Each call returns the next page of results in sequence
     * - Results per page are consistent with the initial search (typically 20-30 items)
     * - The [SearchSession] must be updated with the new `nextPage` token after each call
     * - Pagination continues until `hasNextPage` is `false`
     *
     * ## Session Requirements
     * - The session must have a non-null `nextPage` token
     * - The session's internal extractor must still be valid (not garbage collected)
     * - Each session can only be used for sequential pagination (no parallel calls with same session)
     *
     * ## Error Handling
     * Returns `Result.failure` if:
     * - The session has no more pages available (`nextPage` is null)
     * - Network errors occur and all retry attempts are exhausted
     * - The NewPipe extractor fails to fetch the page
     * - The session's internal state has been invalidated
     *
     * ## Usage Example
     * ```kotlin
     * var session = initialSession
     *
     * while (session.nextPage != null) {
     *     val result = extractor.fetchMore(session).getOrThrow()
     *
     *     // Display new results
     *     displayResults(result.items)
     *
     *     // Update session for next iteration
     *     session = session.copy(nextPage = result.nextPage)
     * }
     * ```
     *
     * @param session The active search session containing the pagination state. Must have a non-null
     * `nextPage` token. This session should come from either an initial [search] call or a previous
     * [fetchMore] call with the `nextPage` updated.
     * @return A [Result] containing the next [SearchResult] page on success, or an exception on failure.
     * The returned result will have `suggestion` and `isCorrected` set to their default values since
     * these are only relevant for the initial search.
     * @throws IllegalStateException if the session has no more pages available or if fetching the page fails.
     *
     * @see search for performing the initial search that creates the session
     * @see SearchSession for managing pagination state
     * @see SearchResult for the structure of returned results
     */
    suspend fun fetchMore(session: SearchSession): Result<SearchResult> =
        withContext(Dispatchers.IO) {
            runWithRetry {
                val nextPage = session.nextPage
                    ?: throw IllegalStateException("No more pages available")

                val pageResult = session.extractor.getPage(nextPage)
                    ?: throw IllegalStateException("Failed to fetch page")

                SearchResult(
                    items = mapItems(pageResult.items),
                    nextPage = pageResult.nextPage,
                    hasNextPage = pageResult.hasNextPage(),
                    suggestion = null,
                    isCorrected = false,
                    query = session.query
                )
            }
        }

    /**
     * Retrieves autocomplete search suggestions based on partial query input.
     *
     * This method provides real-time search suggestions as the user types, similar to YouTube's
     * search bar autocomplete functionality. It's designed to be called on each keystroke or after
     * a short debounce delay to provide responsive search assistance.
     *
     * The method executes on the IO dispatcher and applies retry logic for reliability. It includes
     * an optimization to avoid unnecessary API calls for very short queries (less than 2 characters),
     * returning an empty list immediately in such cases.
     *
     * ## Suggestion Behavior
     * - Suggestions are based on popular searches, trending topics, and user search history
     * - Minimum query length: 2 characters (shorter queries return empty list without API call)
     * - Suggestions are returned in order of relevance/popularity
     * - The number of suggestions varies but is typically 5-10 items
     * - Suggestions are language-aware and may vary by region
     *
     * ## Performance Considerations
     * - This method is designed for frequent calls (on each keystroke)
     * - Consider debouncing calls (e.g., 300ms delay) to reduce API load
     * - Short queries (< 2 chars) skip the API call entirely for efficiency
     * - Suggestions are cached by YouTube's API for commonly searched terms
     *
     * ## Error Handling
     * Returns `Result.failure` if:
     * - Network errors occur and all retry attempts are exhausted
     * - The suggestion service is unavailable
     * - NewPipe extraction fails for any reason
     *
     * Returns an empty list (not an error) if:
     * - The query is shorter than 2 characters
     * - No suggestions are available for the query
     * - The service returns null (gracefully handled)
     *
     * ## Usage Example
     * ```kotlin
     * // In a search bar's text change listener
     * searchInput.addTextChangedListener { text ->
     *     lifecycleScope.launch {
     *         // Debounce to avoid excessive calls
     *         delay(300)
     *
     *         val suggestions = extractor.suggestions(text.toString())
     *             .getOrDefault(emptyList())
     *
     *         displaySuggestions(suggestions)
     *     }
     * }
     * ```
     *
     * ## Integration with Search
     * ```kotlin
     * // User selects a suggestion
     * onSuggestionSelected { suggestion ->
     *     val (result, session) = extractor.search(suggestion).getOrThrow()
     *     displayResults(result)
     * }
     * ```
     *
     * @param query The partial search query for which to retrieve suggestions. Queries shorter than
     * 2 characters will immediately return an empty list without making an API call.
     * @return A [Result] containing a list of suggestion strings on success, or an exception on failure.
     * Returns an empty list (wrapped in success) for short queries or when no suggestions are available.
     *
     * @see search for performing a full search with a selected suggestion
     */
    suspend fun suggestions(query: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            // Short-circuit for very short queries to avoid unnecessary API calls
            if (query.trim().length < 2) {
                return@withContext Result.success(emptyList())
            }

            runWithRetry {
                val service = NewPipe.getService(serviceId)
                service.suggestionExtractor
                    ?.suggestionList(query)
                    ?.toList()
                    ?: emptyList()
            }
        }

    /**
     * Maps NewPipe [InfoItem] instances to unified [StreamItem] instances.
     *
     * This internal helper method converts the heterogeneous collection of NewPipe InfoItem objects
     * into a homogeneous list of StreamItem instances, filtering out any unsupported types or items
     * that fail to convert. It provides a consistent interface for search results regardless of the
     * underlying content type.
     *
     * ## Conversion Behavior
     * - [StreamInfoItem] → [StreamItem] with type VIDEO
     * - [ChannelInfoItem] → [StreamItem] with type CHANNEL
     * - [PlaylistInfoItem] → [StreamItem] with type PLAYLIST
     * - Other types or failed conversions → Silently filtered out (not included in results)
     *
     * ## Error Handling
     * The method uses `runCatching` with `getOrNull()` to ensure that conversion errors for individual
     * items don't break the entire result set. Items that fail to convert are simply excluded from the
     * final list, allowing partial results even if some items are problematic.
     *
     * @param items The raw list of NewPipe [InfoItem] instances from a search or pagination result.
     * @return A filtered list of successfully converted [StreamItem] instances. May be smaller than
     * the input list if some items failed to convert or were of unsupported types.
     *
     * @see StreamItem.from for the conversion logic for each item type
     */
    private fun mapItems(items: List<InfoItem>): List<StreamItem> {
        return items.mapNotNull { item ->
            runCatching {
                when (item) {
                    is StreamInfoItem -> StreamItem.from(item)
                    is ChannelInfoItem -> StreamItem.from(item)
                    is PlaylistInfoItem -> StreamItem.from(item)
                    else -> null
                }
            }.getOrNull()
        }
    }

    /**
     * Executes a suspending operation with automatic retry logic and exponential backoff.
     *
     * This internal utility method provides robust error recovery for network operations by
     * automatically retrying failed attempts with increasing delays. It implements an exponential
     * backoff strategy to avoid overwhelming the server and improve success rates for transient failures.
     *
     * ## Retry Behavior
     * - **Initial Attempt**: Executes immediately without delay
     * - **Retry Attempts**: Up to [SearchConfig.maxRetries] additional attempts
     * - **Delay Strategy**: Exponentially increasing: `retryDelayMillis * (attempt + 1)`
     * - **Total Attempts**: 1 + maxRetries (e.g., 4 total attempts with maxRetries = 3)
     *
     * ## Example Retry Timeline
     * With default settings (maxRetries = 3, retryDelayMillis = 500):
     * - Attempt 1: Immediate
     * - Attempt 2: After 500ms delay
     * - Attempt 3: After 1000ms delay
     * - Attempt 4: After 1500ms delay
     *
     * ## Error Handling
     * - All exceptions are caught and trigger retry logic
     * - The last exception is returned if all attempts fail
     * - If an unknown error occurs (highly unlikely), returns [IllegalStateException]
     *
     * @param T The return type of the operation
     * @param block The suspending operation to execute with retry logic. This should be the actual
     * extraction or network operation that may fail.
     * @return A [Result] containing the successful result or the last exception encountered.
     *
     * @see SearchConfig.maxRetries for configuring retry attempts
     * @see SearchConfig.retryDelayMillis for configuring retry delays
     */
    private suspend fun <T> runWithRetry(block: suspend () -> T): Result<T> {
        var lastError: Throwable? = null

        repeat(config.maxRetries + 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastError = e
                // Only delay if there are more attempts remaining
                if (attempt < config.maxRetries) {
                    // Exponential backoff: delay increases with each attempt
                    delay(config.retryDelayMillis * (attempt + 1))
                }
            }
        }

        // All retry attempts exhausted, return the last error
        return Result.failure(
            lastError ?: IllegalStateException("Unknown error after ${config.maxRetries} retries")
        )
    }
}