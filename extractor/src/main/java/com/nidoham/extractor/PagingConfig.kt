package com.nidoham.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Configuration for stream pagination.
 */
data class PagingConfig(
    val pageSize: Int = 10,
    val maxRetries: Int = 3,
    val retryDelayMillis: Long = 1000L
)

/**
 * Paginated state for stream items.
 */
data class PagingState<T>(
    val items: List<T> = emptyList(),
    val nextPage: Page? = null,
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: Throwable? = null
)

/**
 * Extracts stream information with paging support for related streams.
 */
class StreamExtractor(
    private val config: PagingConfig = PagingConfig()
) {
    private val service = ServiceList.YouTube

    /**
     * Fetches stream details with initial related items.
     */
    suspend fun fetchStream(url: String): Result<StreamData> = withContext(Dispatchers.IO) {
        runCatching {
            val info = StreamInfo.getInfo(service, url)
            val main = Streams.from(info)
            
            val related = info.relatedItems
                ?.take(config.pageSize)
                ?.filterIsInstance<StreamInfoItem>()
                ?.map { StreamItem.from(it) }
                ?: emptyList()

            val hasMore = (info.relatedItems?.size ?: 0) > config.pageSize
            val nextPage = if (hasMore) Page(url, config.pageSize.toString()) else null

            StreamData(main, related, nextPage)
        }
    }

    /**
     * Creates Flow for paginated related streams.
     */
    fun fetchRelatedPages(url: String): Flow<PagingState<StreamItem>> = flow {
        var currentPage: Page? = Page(url, "0")
        val allItems = mutableListOf<StreamItem>()

        emit(PagingState(isLoading = true))

        while (currentPage != null) {
            val result = fetchPage(url, currentPage)

            result.fold(
                onSuccess = { page ->
                    allItems.addAll(page.items)
                    currentPage = page.nextPage

                    emit(PagingState(
                        items = allItems.toList(),
                        nextPage = currentPage,
                        isLoading = false,
                        isComplete = page.isLastPage
                    ))

                    if (page.isLastPage) break
                },
                onFailure = { error ->
                    emit(PagingState(
                        items = allItems.toList(),
                        nextPage = currentPage,
                        isLoading = false,
                        error = error
                    ))
                    return@flow
                }
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Internal: Fetches single page of related streams.
     */
    private suspend fun fetchPage(
        url: String,
        page: Page
    ): Result<PageResult<StreamItem>> = withContext(Dispatchers.IO) {
        runWithRetry {
            val info = StreamInfo.getInfo(service, url)
            val offset = page.id.toIntOrNull() ?: 0
            
            val items = info.relatedItems
                ?.filterIsInstance<StreamInfoItem>()
                ?.drop(offset)
                ?.take(config.pageSize)
                ?.map { StreamItem.from(it) }
                ?: emptyList()

            val hasMore = (info.relatedItems?.size ?: 0) > offset + config.pageSize
            val nextPage = if (hasMore) Page(url, (offset + config.pageSize).toString()) else null

            PageResult(items, nextPage)
        }
    }

    /**
     * Internal: Retry logic.
     */
    private suspend fun <T> runWithRetry(block: suspend () -> T): Result<T> {
        var lastError: Throwable? = null
        repeat(config.maxRetries + 1) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastError = e
                if (attempt < config.maxRetries) {
                    kotlinx.coroutines.delay(config.retryDelayMillis * (attempt + 1))
                }
            }
        }
        return Result.failure(lastError ?: IllegalStateException("Failed"))
    }

    /**
     * Internal: Page result container.
     */
    private data class PageResult<T>(
        val items: List<T>,
        val nextPage: Page?
    ) {
        val isLastPage: Boolean get() = nextPage == null
    }
}

/**
 * Stream data container.
 */
data class StreamData(
    val stream: Streams,
    val relatedItems: List<StreamItem>,
    val nextPage: Page? = null
)