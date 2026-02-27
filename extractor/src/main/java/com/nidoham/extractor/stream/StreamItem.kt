package com.nidoham.extractor.stream

import com.nidoham.extractor.safety.engine.RestrictedEngine
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * A unified model representing different types of streaming content including videos, channels, and playlists.
 *
 * Acts as an adapter layer between NewPipe's heterogeneous [InfoItem] subtypes and the
 * application's domain model, providing a single consistent structure for UI rendering.
 *
 * @property name Display name — video title, channel name, or playlist name.
 * @property url Canonical URL to the content on the platform.
 * @property thumbnails Available thumbnails in varying resolutions.
 * @property type Content type — drives conditional rendering and navigation.
 * @property uploaderName Channel/creator name. Mirrors [name] for channel items.
 * @property uploaderUrl URL to the uploader's profile. Mirrors [url] for channel items.
 * @property duration Video duration in seconds. `-1` when unavailable or not applicable.
 * @property viewCount Video view count. `-1` when unavailable or not applicable.
 * @property uploadDate Upload date wrapped for localization. `null` when unavailable or not applicable.
 * @property isShort Whether the video is a YouTube Short (vertical short-form content).
 * @property subscriberCount Channel subscriber count. `-1` when unavailable or not applicable.
 * @property description Channel description. Empty for non-channel items.
 * @property streamCount Number of videos in a playlist. `-1` when unavailable or not applicable.
 * @property verified Whether the uploader has a platform verification badge.
 * @property isAgeRestricted Whether [RestrictedEngine] flagged this video's metadata as restricted.
 *           Determined solely by content analysis since NewPipe's StreamInfoItem exposes no
 *           platform-level age-restriction flag.
 */
data class StreamItem(
    val name: String,
    val url: String,
    val thumbnails: List<Image>,
    val type: ItemType,
    val uploaderName: String,
    val uploaderUrl: String,
    val duration: Long = -1L,
    val viewCount: Long = -1L,
    val uploadDate: DateWrapper? = null,
    val isShort: Boolean = false,
    val subscriberCount: Long = -1L,
    val description: String = "",
    val streamCount: Long = -1L,
    val verified: Boolean = false,
    val isAgeRestricted: Boolean = false
) {

    /**
     * Discriminates content types for display logic and navigation.
     * Each variant maps to a specific NewPipe [InfoItem] subclass.
     */
    enum class ItemType {
        /** Playable video — has duration, view count, upload date. Mapped from [StreamInfoItem]. */
        VIDEO,
        /** Creator profile — has subscriber count and description. Mapped from [ChannelInfoItem]. */
        CHANNEL,
        /** Video collection — has stream count. Mapped from [PlaylistInfoItem]. */
        PLAYLIST,
        /** Fallback for unrecognised types; ensures graceful degradation. */
        UNKNOWN
    }

    companion object {

        /**
         * Primary factory — converts any NewPipe [InfoItem] into a [StreamItem].
         * Always returns a valid instance; unknown types fall back to [createUnknown].
         */
        fun from(item: InfoItem): StreamItem = when (item) {
            is StreamInfoItem   -> fromStream(item)
            is ChannelInfoItem  -> fromChannel(item)
            is PlaylistInfoItem -> fromPlaylist(item)
            else                -> createUnknown(item)
        }

        /**
         * Maps a [StreamInfoItem] (video) to [StreamItem].
         *
         * [isAgeRestricted] is driven entirely by [RestrictedEngine] — NewPipe's
         * [StreamInfoItem] exposes no age-restriction flag and no tags list at the
         * InfoItem level, so title + uploader name are the fields we can actually use.
         */
        private fun fromStream(item: StreamInfoItem): StreamItem {
            val title    = item.name.orEmpty()
            val uploader = item.uploaderName.orEmpty()

            return StreamItem(
                type            = ItemType.VIDEO,
                name            = title,
                url             = item.url.orEmpty(),
                thumbnails      = item.thumbnails,
                uploaderName    = uploader,
                uploaderUrl     = item.uploaderUrl.orEmpty(),
                duration        = item.duration.takeIf { it >= 0L } ?: -1L,
                viewCount       = item.viewCount.takeIf { it >= 0L } ?: -1L,
                uploadDate      = item.uploadDate,
                isShort         = item.isShortFormContent,
                verified        = item.isUploaderVerified,
                isAgeRestricted = RestrictedEngine.checkYouTubeMetadata(
                    title       = title,
                    description = uploader  // only extra text field available at InfoItem level
                ).isBlocked
            )
        }

        /**
         * Maps a [ChannelInfoItem] to [StreamItem].
         * Uploader fields mirror the channel's own name/URL per the unified model contract.
         */
        private fun fromChannel(item: ChannelInfoItem): StreamItem = StreamItem(
            type            = ItemType.CHANNEL,
            name            = item.name.orEmpty(),
            url             = item.url.orEmpty(),
            thumbnails      = item.thumbnails,
            uploaderName    = item.name.orEmpty(),
            uploaderUrl     = item.url.orEmpty(),
            subscriberCount = item.subscriberCount.takeIf { it >= 0L } ?: -1L,
            description     = item.description.orEmpty(),
            verified        = item.isVerified
        )

        /**
         * Maps a [PlaylistInfoItem] to [StreamItem].
         */
        private fun fromPlaylist(item: PlaylistInfoItem): StreamItem = StreamItem(
            type         = ItemType.PLAYLIST,
            name         = item.name.orEmpty(),
            url          = item.url.orEmpty(),
            thumbnails   = item.thumbnails,
            uploaderName = item.uploaderName.orEmpty(),
            uploaderUrl  = item.uploaderUrl.orEmpty(),
            streamCount  = item.streamCount.takeIf { it >= 0L } ?: -1L
        )

        /**
         * Fallback for unrecognised [InfoItem] types.
         * Populates only the fields available on the base [InfoItem] interface.
         */
        private fun createUnknown(item: InfoItem): StreamItem = StreamItem(
            type         = ItemType.UNKNOWN,
            name         = item.name.orEmpty(),
            url          = item.url.orEmpty(),
            thumbnails   = item.thumbnails,
            uploaderName = "",
            uploaderUrl  = ""
        )
    }
}