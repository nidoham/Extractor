package com.nidoham.extractor.stream

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * A unified model representing different types of streaming content including videos, channels, and playlists.
 *
 * This data class provides a common interface for handling various content types from the NewPipe extractor,
 * simplifying UI rendering and data processing by normalizing different content types into a single, consistent structure.
 * It acts as an adapter layer between NewPipe's heterogeneous content types and the application's domain model.
 *
 * ## Usage Example
 * ```kotlin
 * val infoItem: InfoItem = // ... obtained from NewPipe
 * val streamItem = StreamItem.from(infoItem)
 *
 * when (streamItem.type) {
 *     ItemType.VIDEO -> displayVideo(streamItem)
 *     ItemType.CHANNEL -> displayChannel(streamItem)
 *     ItemType.PLAYLIST -> displayPlaylist(streamItem)
 *     ItemType.UNKNOWN -> handleUnknownType(streamItem)
 * }
 * ```
 *
 * @property name The display name of the content. This represents the video title, channel name, or playlist name
 * depending on the content type.
 * @property url The canonical URL to access the content directly on the platform.
 * @property thumbnails List of thumbnail images available for the content in different resolutions.
 * The list is ordered by quality, with higher resolution images typically appearing first.
 * @property type The type of content this item represents. Used for conditional rendering and navigation logic.
 * @property uploaderName The name of the content creator. For videos, this is the channel name; for playlists,
 * this is the playlist creator's name; for channels, this mirrors the channel name.
 * @property uploaderUrl The URL to the uploader's channel or profile page.
 * @property duration Duration of the video in seconds. Returns `-1` for non-video items or when duration is unavailable.
 * @property viewCount Number of views for the video. Returns `-1` for non-video items or when view count is unavailable.
 * @property uploadDate The date when the video was uploaded, wrapped in NewPipe's [DateWrapper] for proper localization.
 * Returns `null` for non-video items or when upload date is unavailable.
 * @property isShort Indicates whether the video is a YouTube Short (vertical short-form content). Always `false` for non-video items.
 * @property subscriberCount Number of subscribers for channels. Returns `-1` for non-channel items or when subscriber count is unavailable.
 * @property description Channel description text. Returns an empty string for non-channel items.
 * @property streamCount Number of videos contained in a playlist. Returns `-1` for non-playlist items or when stream count is unavailable.
 * @property verified Indicates whether the uploader has platform verification (e.g., YouTube verification badge).
 *
 * @see ItemType for the different content types supported
 * @see from for converting NewPipe InfoItems to StreamItem instances
 */
data class StreamItem(
    val name: String,
    val url: String,
    val thumbnails: List<Image>,
    val type: ItemType,
    val uploaderName: String,
    val uploaderUrl: String,
    val duration: Long = -1,
    val viewCount: Long = -1,
    val uploadDate: DateWrapper? = null,
    val isShort: Boolean = false,
    val subscriberCount: Long = -1,
    val description: String = "",
    val streamCount: Long = -1,
    val verified: Boolean = false
) {

    /**
     * Defines the type of streaming content for UI rendering and conditional logic.
     *
     * This enumeration helps differentiate between different content types to apply appropriate
     * display logic, navigation behavior, and data handling. Each type corresponds to a specific
     * NewPipe InfoItem subclass.
     */
    enum class ItemType {
        /**
         * Represents a video stream item.
         *
         * Videos contain playable media content with duration, view count, and upload date.
         * Mapped from [StreamInfoItem].
         */
        VIDEO,

        /**
         * Represents a channel or creator profile.
         *
         * Channels contain subscriber count and description information.
         * Mapped from [ChannelInfoItem].
         */
        CHANNEL,

        /**
         * Represents a collection of videos (playlist).
         *
         * Playlists contain a stream count indicating the number of videos.
         * Mapped from [PlaylistInfoItem].
         */
        PLAYLIST,

        /**
         * Represents an unrecognized or unsupported content type.
         *
         * Used as a fallback when the InfoItem type doesn't match any known type,
         * ensuring graceful degradation without crashes.
         */
        UNKNOWN
    }

    companion object {

        /**
         * Factory method to convert NewPipe's generic [InfoItem] into a unified [StreamItem].
         *
         * This method automatically detects the runtime type of the InfoItem and delegates to the
         * appropriate mapper function to create a properly configured StreamItem instance. It serves
         * as the primary entry point for creating StreamItem instances from NewPipe data.
         *
         * The method handles the following types:
         * - [StreamInfoItem] → VIDEO type with duration, views, and upload date
         * - [ChannelInfoItem] → CHANNEL type with subscriber count and description
         * - [PlaylistInfoItem] → PLAYLIST type with stream count
         * - Unknown types → UNKNOWN type with minimal data
         *
         * @param item The NewPipe [InfoItem] to convert. Must not be null.
         * @return A [StreamItem] instance with all relevant fields populated based on the item type.
         * Returns a valid StreamItem even for unknown types to ensure application stability.
         *
         * @see fromStream for video-specific conversion
         * @see fromChannel for channel-specific conversion
         * @see fromPlaylist for playlist-specific conversion
         * @see createUnknown for fallback conversion
         */
        fun from(item: InfoItem): StreamItem {
            return when (item) {
                is StreamInfoItem -> fromStream(item)
                is ChannelInfoItem -> fromChannel(item)
                is PlaylistInfoItem -> fromPlaylist(item)
                else -> createUnknown(item)
            }
        }

        /**
         * Converts a [StreamInfoItem] (video) into a [StreamItem].
         *
         * Maps video-specific properties including duration, view count, and upload date.
         * Handles edge cases where values might be negative or missing by providing sensible defaults.
         *
         * @param item The [StreamInfoItem] to convert. Must not be null.
         * @return A [StreamItem] configured with video-specific data and type set to [ItemType.VIDEO].
         * Returns `0L` for duration and view count when values are invalid or missing.
         */
        private fun fromStream(item: StreamInfoItem): StreamItem {
            return StreamItem(
                type = ItemType.VIDEO,
                name = item.name.orEmpty(),
                url = item.url.orEmpty(),
                thumbnails = item.thumbnails,
                uploaderName = item.uploaderName.orEmpty(),
                uploaderUrl = item.uploaderUrl.orEmpty(),
                duration = item.duration.takeIf { it > 0 } ?: 0L,
                viewCount = item.viewCount.takeIf { it >= 0 } ?: 0L,
                uploadDate = item.uploadDate,
                verified = item.isUploaderVerified
            )
        }

        /**
         * Converts a [ChannelInfoItem] into a [StreamItem].
         *
         * Maps channel-specific properties including subscriber count and description.
         * For channels, the uploader fields are set to reference the channel itself,
         * maintaining consistency with the data model.
         *
         * @param item The [ChannelInfoItem] to convert. Must not be null.
         * @return A [StreamItem] configured with channel-specific data and type set to [ItemType.CHANNEL].
         * Returns `0L` for subscriber count when the value is invalid or missing.
         */
        private fun fromChannel(item: ChannelInfoItem): StreamItem {
            return StreamItem(
                type = ItemType.CHANNEL,
                name = item.name.orEmpty(),
                url = item.url.orEmpty(),
                thumbnails = item.thumbnails,
                uploaderName = item.name.orEmpty(),
                uploaderUrl = item.url.orEmpty(),
                subscriberCount = item.subscriberCount.takeIf { it >= 0 } ?: 0L,
                description = item.description.orEmpty(),
                verified = item.isVerified
            )
        }

        /**
         * Converts a [PlaylistInfoItem] into a [StreamItem].
         *
         * Maps playlist-specific properties including the stream count (number of videos in the playlist).
         * Preserves uploader information for playlists created by specific users or channels.
         *
         * @param item The [PlaylistInfoItem] to convert. Must not be null.
         * @return A [StreamItem] configured with playlist-specific data and type set to [ItemType.PLAYLIST].
         * Returns `0L` for stream count when the value is invalid or missing.
         */
        private fun fromPlaylist(item: PlaylistInfoItem): StreamItem {
            return StreamItem(
                type = ItemType.PLAYLIST,
                name = item.name.orEmpty(),
                url = item.url.orEmpty(),
                thumbnails = item.thumbnails,
                uploaderName = item.uploaderName.orEmpty(),
                uploaderUrl = item.uploaderUrl.orEmpty(),
                streamCount = item.streamCount.takeIf { it >= 0 } ?: 0L
            )
        }

        /**
         * Creates a fallback [StreamItem] for unrecognized [InfoItem] types.
         *
         * Populates only the common fields (name, url, thumbnails) that are available on the base
         * [InfoItem] interface and sets the type to [ItemType.UNKNOWN]. This ensures the application
         * can handle unexpected content types gracefully without crashing, though with limited functionality.
         *
         * @param item The unrecognized [InfoItem]. Must not be null.
         * @return A [StreamItem] with type [ItemType.UNKNOWN] and minimal data populated.
         * Uploader fields are set to empty strings as this information is not available on the base interface.
         */
        private fun createUnknown(item: InfoItem): StreamItem {
            return StreamItem(
                type = ItemType.UNKNOWN,
                name = item.name.orEmpty(),
                url = item.url.orEmpty(),
                thumbnails = item.thumbnails,
                uploaderName = "",
                uploaderUrl = ""
            )
        }
    }
}