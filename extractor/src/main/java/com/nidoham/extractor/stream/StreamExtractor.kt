package com.nidoham.extractor.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Configuration parameters for stream pagination and retry behavior.
 *
 * This data class encapsulates the configurable aspects of stream extraction, including
 * pagination limits and error recovery strategies. These settings allow fine-tuning of
 * the extraction process to balance between performance, reliability, and resource usage.
 *
 * ## Usage Example
 * ```kotlin
 * val config = PagingConfig(
 *     pageSize = 20,           // Load 20 items per page
 *     maxRetries = 5,          // Retry failed requests up to 5 times
 *     retryDelayMillis = 2000L // Wait 2 seconds between retries
 * )
 * val extractor = StreamExtractor(config)
 * ```
 *
 * @property pageSize The number of related stream items to load per page. Determines the pagination
 * granularity. Larger values reduce the number of pagination calls but increase memory usage and initial
 * load time. Default is `10` items per page.
 * @property maxRetries The maximum number of retry attempts for failed network requests before giving up.
 * Each retry applies an exponentially increasing delay. Default is `3` retries, allowing up to 4 total attempts
 * (initial attempt + 3 retries).
 * @property retryDelayMillis The base delay in milliseconds between retry attempts. The actual delay increases
 * exponentially with each retry: `retryDelayMillis * (attempt + 1)`. For example, with default `1000L`,
 * delays will be 1s, 2s, 3s for subsequent retries. Default is `1000L` (1 second).
 */
data class PagingConfig(
    val pageSize: Int = 10,
    val maxRetries: Int = 3,
    val retryDelayMillis: Long = 1000L
)

/**
 * Extracts comprehensive stream information with pagination support for related content.
 *
 * This class provides a robust interface for fetching detailed stream metadata and related content
 * from YouTube (via NewPipe) with built-in pagination, error handling, and retry logic. It handles
 * the complexity of NewPipe's extraction API and provides a clean, Kotlin-idiomatic interface with
 * coroutine support for asynchronous operations.
 *
 * The extractor operates in two phases:
 * 1. **Initial Fetch**: Retrieves full stream details with the first page of related items
 * 2. **Pagination**: Loads subsequent pages of related items on demand
 *
 * ## Key Features
 * - **Automatic Retry**: Failed requests are automatically retried with exponential backoff
 * - **Pagination Support**: Related items are loaded in configurable page sizes
 * - **Error Recovery**: Comprehensive error handling with detailed Result types
 * - **Coroutine Integration**: All operations use Kotlin coroutines with IO dispatcher
 * - **Memory Efficiency**: Large related item lists are paginated to avoid memory issues
 *
 * ## Usage Example
 * ```kotlin
 * val extractor = StreamExtractor(
 *     config = PagingConfig(pageSize = 15, maxRetries = 5)
 * )
 *
 * // Fetch initial stream data
 * val result = extractor.fetchStream("https://www.youtube.com/watch?v=...")
 * result.fold(
 *     onSuccess = { streamData ->
 *         println("Title: ${streamData.stream.title}")
 *         println("Related items: ${streamData.relatedItems.size}")
 *
 *         // Load next page if available
 *         if (streamData.nextPage != null) {
 *             val nextPage = extractor.fetchMoreRelated(
 *                 url = streamData.stream.url,
 *                 currentOffset = streamData.relatedItems.size,
 *                 totalItems = streamData.totalRelatedItems
 *             )
 *         }
 *     },
 *     onFailure = { error ->
 *         println("Failed to fetch stream: ${error.message}")
 *     }
 * )
 * ```
 *
 * ## Thread Safety
 * This class is thread-safe. All extraction operations automatically execute on the IO dispatcher,
 * making it safe to call from any coroutine context including the main thread.
 *
 * @property config The pagination and retry configuration. Defaults to standard settings if not provided.
 *
 * @see PagingConfig for configuration options
 * @see StreamData for the structure of returned stream data
 * @see RelatedPageResult for the structure of paginated results
 */
class StreamExtractor(
    private val config: PagingConfig = PagingConfig()
) {

    /**
     * The NewPipe service instance used for all extraction operations.
     * Currently hardcoded to YouTube; could be parameterized for multi-platform support.
     */
    private val service = ServiceList.YouTube

    /**
     * Fetches comprehensive stream information including the first page of related items.
     *
     * This method performs the initial extraction of stream data, including all metadata,
     * available media streams, and the first page of related content. It automatically handles
     * network errors with retry logic and provides pagination support for related items.
     *
     * The method executes on the IO dispatcher and blocks the calling coroutine until completion.
     * Failed requests are automatically retried according to the configured retry policy with
     * exponential backoff.
     *
     * ## Returned Data Structure
     * On success, returns [StreamData] containing:
     * - **stream**: Complete stream metadata and media streams
     * - **relatedItems**: First page of related items (size determined by [PagingConfig.pageSize])
     * - **nextPage**: Pagination token for loading more related items (null if no more pages)
     * - **totalRelatedItems**: Complete list of all related items for client-side pagination
     *
     * ## Error Handling
     * Returns `Result.failure` if:
     * - The URL is invalid or malformed
     * - The stream is unavailable, private, or deleted
     * - Network errors occur and all retry attempts are exhausted
     * - NewPipe extraction fails for any reason
     *
     * ## Implementation Notes
     * - Only [StreamInfoItem] types are included in related items; other types are filtered out
     * - The complete list of related items is stored for efficient client-side pagination
     * - The first page is extracted immediately; subsequent pages use [fetchMoreRelated]
     *
     * @param url The YouTube video URL to extract. Must be a valid YouTube video URL
     * (e.g., "https://www.youtube.com/watch?v=VIDEO_ID").
     * @return A [Result] containing [StreamData] on success, or an exception on failure.
     * Check with `result.isSuccess` or use `fold` to handle both cases.
     *
     * @see fetchMoreRelated for loading additional pages of related items
     * @see StreamData for the complete data structure
     * @see PagingConfig for retry and pagination settings
     */
    suspend fun fetchStream(url: String): Result<StreamData> = withContext(Dispatchers.IO) {
        runWithRetry {
            // Extract full stream information from NewPipe
            val info = StreamInfo.getInfo(service, url)
            val main = Streams.from(info)

            // Filter and collect related stream items
            val allRelated = info.relatedItems
                .filterIsInstance<StreamInfoItem>()

            // Extract first page of related items
            val firstPageItems = allRelated.take(config.pageSize)
            val related = firstPageItems.map { StreamItem.from(it) }

            // Determine if more pages exist
            val hasMore = allRelated.size > config.pageSize
            val nextPage = if (hasMore) {
                Page(url, config.pageSize.toString())
            } else {
                null
            }

            StreamData(
                stream = main,
                relatedItems = related,
                nextPage = nextPage,
                totalRelatedItems = allRelated
            )
        }
    }

    /**
     * Fetches the next page of related stream items using client-side pagination.
     *
     * This method implements efficient client-side pagination by extracting items from the
     * complete related items list (obtained in the initial [fetchStream] call). This approach
     * avoids additional network requests while maintaining a paginated interface for the UI.
     *
     * The method executes on the IO dispatcher and applies the same retry logic as [fetchStream],
     * though failures are unlikely since this operates on already-fetched data.
     *
     * ## Pagination Strategy
     * This implementation uses client-side pagination instead of NewPipe's server-side pagination
     * because:
     * 1. YouTube's related items are typically small enough to fetch entirely
     * 2. Eliminates additional network latency for subsequent pages
     * 3. Provides consistent pagination behavior
     * 4. Simplifies error handling (no network errors after initial fetch)
     *
     * ## Usage Pattern
     * ```kotlin
     * val initialData = extractor.fetchStream(url).getOrThrow()
     * var currentOffset = initialData.relatedItems.size
     * var nextPage = initialData.nextPage
     *
     * while (nextPage != null) {
     *     val pageResult = extractor.fetchMoreRelated(
     *         url = initialData.stream.url,
     *         currentOffset = currentOffset,
     *         totalItems = initialData.totalRelatedItems
     *     ).getOrThrow()
     *
     *     // Process pageResult.items
     *     currentOffset = pageResult.nextOffset
     *     nextPage = pageResult.nextPage
     * }
     * ```
     *
     * @param url The stream URL (typically from [StreamData.stream.url]). Used for generating
     * pagination tokens but does not trigger new network requests.
     * @param currentOffset The current pagination offset, indicating how many items have been
     * loaded so far. Should start at the size of [StreamData.relatedItems] from the initial fetch.
     * @param totalItems The complete list of related items from [StreamData.totalRelatedItems].
     * This must be the same list obtained from the initial [fetchStream] call to ensure consistency.
     * @return A [Result] containing [RelatedPageResult] with the next page of items and pagination state.
     * Returns `Result.failure` only if the retry logic encounters unexpected errors.
     *
     * @see fetchStream for the initial data fetch that provides totalItems
     * @see RelatedPageResult for the structure of paginated results
     */
    suspend fun fetchMoreRelated(
        url: String,
        currentOffset: Int,
        totalItems: List<StreamInfoItem>
    ): Result<RelatedPageResult> = withContext(Dispatchers.IO) {
        runWithRetry {
            // Extract next page from the cached total items
            val nextItems = totalItems
                .drop(currentOffset)
                .take(config.pageSize)
                .map { StreamItem.from(it) }

            // Calculate next pagination state
            val nextOffset = currentOffset + config.pageSize
            val hasMore = totalItems.size > nextOffset
            val nextPage = if (hasMore) {
                Page(url, nextOffset.toString())
            } else {
                null
            }

            RelatedPageResult(
                items = nextItems,
                nextPage = nextPage,
                nextOffset = nextOffset,
                hasMore = hasMore
            )
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
     * - **Retry Attempts**: Up to [PagingConfig.maxRetries] additional attempts
     * - **Delay Strategy**: Exponentially increasing: `retryDelayMillis * (attempt + 1)`
     * - **Total Attempts**: 1 + maxRetries (e.g., 4 total attempts with maxRetries = 3)
     *
     * ## Example Retry Timeline
     * With default settings (maxRetries = 3, retryDelayMillis = 1000):
     * - Attempt 1: Immediate
     * - Attempt 2: After 1 second delay
     * - Attempt 3: After 2 second delay
     * - Attempt 4: After 3 second delay
     *
     * ## Error Handling
     * - All exceptions are caught and trigger retry logic
     * - The last exception is returned if all attempts fail
     * - If an unknown error occurs (highly unlikely), returns [IllegalStateException]
     *
     * @param T The return type of the operation
     * @param block The suspending operation to execute with retry logic. This should be the
     * actual extraction or network operation that may fail.
     * @return A [Result] containing the successful result or the last exception encountered.
     *
     * @see PagingConfig.maxRetries for configuring retry attempts
     * @see PagingConfig.retryDelayMillis for configuring retry delays
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

/**
 * Container for comprehensive stream data including metadata and paginated related content.
 *
 * This data class encapsulates all information retrieved from an initial stream fetch,
 * including the main stream details and the first page of related content. It provides
 * everything needed for displaying a video page and implementing pagination for related items.
 *
 * ## Usage Example
 * ```kotlin
 * val streamData: StreamData = extractor.fetchStream(url).getOrThrow()
 *
 * // Display main stream
 * displayStream(streamData.stream)
 *
 * // Display first page of related items
 * displayRelatedItems(streamData.relatedItems)
 *
 * // Check if pagination is needed
 * if (streamData.nextPage != null) {
 *     loadMoreButton.show()
 * }
 * ```
 *
 * @property stream The complete stream metadata including all available media streams, upload information,
 * and channel details. This is the primary content being viewed.
 * @property relatedItems The first page of related stream items, ready for display. The size is determined
 * by [PagingConfig.pageSize]. These are fully converted [StreamItem] instances.
 * @property nextPage Pagination token for loading the next page of related items. Returns `null` if all
 * related items fit in the first page (i.e., total items ≤ pageSize). Pass to [StreamExtractor.fetchMoreRelated].
 * @property totalRelatedItems The complete list of all related items as [StreamInfoItem] instances. Store this
 * for use in subsequent pagination calls. This enables efficient client-side pagination without additional
 * network requests.
 *
 * @see Streams for the main stream data structure
 * @see StreamItem for the related items structure
 * @see StreamExtractor.fetchStream for obtaining this data
 * @see StreamExtractor.fetchMoreRelated for loading additional pages
 */
data class StreamData(
    val stream: Streams,
    val relatedItems: List<StreamItem>,
    val nextPage: Page?,
    val totalRelatedItems: List<StreamInfoItem>
)

/**
 * Result of fetching a page of related stream items.
 *
 * This data class represents the outcome of a pagination request, containing the loaded items
 * and the state needed to continue pagination. It provides all information necessary to update
 * the UI and determine if more pages are available.
 *
 * ## Usage Example
 * ```kotlin
 * val pageResult: RelatedPageResult = extractor.fetchMoreRelated(
 *     url = streamUrl,
 *     currentOffset = currentOffset,
 *     totalItems = allRelatedItems
 * ).getOrThrow()
 *
 * // Append new items to the list
 * relatedItemsList.addAll(pageResult.items)
 *
 * // Update pagination state
 * currentOffset = pageResult.nextOffset
 *
 * // Check if more pages exist
 * if (pageResult.hasMore) {
 *     loadMoreButton.show()
 * } else {
 *     loadMoreButton.hide()
 * }
 * ```
 *
 * @property items The list of related stream items for this page. The size is determined by
 * [PagingConfig.pageSize], except possibly for the last page which may contain fewer items.
 * @property nextPage Pagination token for loading the next page. Returns `null` if this is the last page.
 * Pass to [StreamExtractor.fetchMoreRelated] to load the next page.
 * @property nextOffset The offset to use for the next pagination request. This value equals
 * `currentOffset + items.size` and should be passed to the next [StreamExtractor.fetchMoreRelated] call.
 * @property hasMore Indicates whether additional pages are available. Equivalent to `nextPage != null`,
 * provided for convenience. When `false`, this is the final page of related items.
 *
 * @see StreamItem for the structure of individual items
 * @see StreamExtractor.fetchMoreRelated for loading additional pages
 */
data class RelatedPageResult(
    val items: List<StreamItem>,
    val nextPage: Page?,
    val nextOffset: Int,
    val hasMore: Boolean
)