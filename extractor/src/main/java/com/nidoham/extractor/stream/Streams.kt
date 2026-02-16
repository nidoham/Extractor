package com.nidoham.extractor.stream

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import java.time.OffsetDateTime

/**
 * Immutable data class representing comprehensive stream metadata and media streams.
 *
 * This class serves as a clean, type-safe wrapper around NewPipe's [StreamInfo] with improved naming
 * conventions and a more idiomatic Kotlin API. It provides a complete, immutable representation of a media
 * stream including all available quality variants, audio tracks, and related metadata. This is the primary
 * data model for full stream details extracted from platforms like YouTube, SoundCloud, or other supported services.
 *
 * The class consolidates all stream-related information including:
 * - Basic metadata (title, description, duration, statistics)
 * - Media streams in various formats and qualities
 * - Uploader and channel information
 * - Related content and recommendations
 * - Upload timing and categorization
 * - Platform-specific details
 *
 * ## Usage Example
 * ```kotlin
 * val streamInfo: StreamInfo = // ... obtained from NewPipe extractor
 * val streams = Streams.from(streamInfo)
 *
 * // Access video streams
 * val bestVideo = streams.videoStreams.maxByOrNull { it.resolution }
 *
 * // Access audio streams
 * val bestAudio = streams.audioStreams.maxByOrNull { it.averageBitrate }
 *
 * // Check if adaptive streaming is available
 * if (streams.dashMpdUrl != null) {
 *     // Use DASH playback
 * }
 * ```
 *
 * @property id The unique identifier for this stream on the platform (e.g., YouTube video ID).
 * @property title The display title of the stream as shown to users.
 * @property description The stream description, which may contain formatted text, timestamps, links, or other segments.
 * This is a NewPipe [Description] object that preserves formatting information.
 * @property duration The length of the stream in seconds. Returns [StreamInfo.UNKNOWN_DURATION] (-1) if the duration
 * cannot be determined (e.g., for live streams or when the information is unavailable).
 * @property viewCount The total number of times this stream has been viewed. May be `-1` if unavailable or hidden by the platform.
 * @property likeCount The number of likes or upvotes received. May be `-1` if unavailable or hidden by the platform.
 * @property dislikeCount The number of dislikes or downvotes received. Often `0` or `-1` for platforms that have hidden
 * this metric (e.g., YouTube post-2021).
 * @property url The canonical URL pointing directly to the stream on the platform.
 * @property thumbnails List of available thumbnail images in various resolutions. Usually ordered from highest to lowest quality.
 * @property dashMpdUrl URL to the DASH (Dynamic Adaptive Streaming over HTTP) MPD manifest for adaptive streaming.
 * Returns `null` if DASH streaming is not available for this content.
 * @property hlsUrl URL to the HLS (HTTP Live Streaming) playlist for adaptive streaming. Returns `null` if HLS streaming
 * is not available for this content.
 * @property audioStreams List of available audio-only streams with different bitrates and formats. Useful for audio-only
 * playback or when combining with video-only streams for optimal quality.
 * @property videoStreams List of available video streams that include both video and audio (progressive download format).
 * These streams can be played directly without requiring separate audio.
 * @property videoOnlyStreams List of video-only streams without audio tracks. These are typically used in conjunction with
 * [audioStreams] for DASH adaptive playback to achieve higher quality than progressive streams.
 * @property uploaderName The display name of the content creator or channel.
 * @property uploaderUrl The URL to the uploader's channel or profile page on the platform.
 * @property uploaderAvatars List of avatar images for the uploader in various resolutions.
 * @property isUploaderVerified Indicates whether the uploader has platform verification (e.g., YouTube verification badge,
 * indicating authenticity of the channel).
 * @property subChannelName Name of the sub-channel or series if applicable. Returns `null` if the content is not part of
 * a sub-channel or series. Some platforms allow channels to create sub-channels or series for content organization.
 * @property subChannelUrl URL to the sub-channel or series page. Returns `null` if not applicable.
 * @property subChannelAvatars Avatar images for the sub-channel. Returns an empty list if not applicable.
 * @property relatedItems List of related videos, channels, or playlists recommended by the platform. These are represented
 * as [InfoItem] objects that can be converted to [StreamItem] for consistent handling.
 * @property nextStreams List of upcoming or queued streams for playlist continuation or auto-play. Represented as [InfoItem]
 * objects. This is particularly useful for implementing "play next" functionality.
 * @property uploadDate The parsed upload date and time with timezone information. Returns `null` if the upload date cannot
 * be determined or parsed. Uses Java 8+ [OffsetDateTime] for proper timezone handling.
 * @property textualUploadDate The raw date string as provided by the platform (e.g., "2 days ago", "March 15, 2024").
 * Returns `null` if not available. This is useful for displaying relative dates in the UI.
 * @property category The content category assigned by the platform (e.g., "Music", "Gaming", "Education", "Entertainment").
 * The exact categories available depend on the platform.
 * @property tags List of tags or keywords associated with the stream. These are typically set by the uploader for
 * discoverability and categorization.
 * @property licence The license type or copyright information (e.g., "YouTube Standard License", "Creative Commons BY",
 * "Creative Commons BY-SA"). The format depends on the platform's licensing system.
 * @property host The streaming platform's host domain (e.g., "youtube.com", "soundcloud.com"). Useful for platform-specific
 * handling or analytics.
 * @property privacy The privacy setting of the stream, indicating whether it's public, unlisted, private, or another
 * platform-specific privacy level. Uses NewPipe's [StreamExtractor.Privacy] enum.
 *
 * @see StreamInfo for the original NewPipe data structure
 * @see StreamItem for a lighter-weight representation of stream items in lists
 * @see from for creating instances from StreamInfo
 */
data class Streams(
    val id: String,
    val title: String,
    val description: Description,
    val duration: Long,
    val viewCount: Long,
    val likeCount: Long,
    val dislikeCount: Long,
    val url: String,
    val thumbnails: List<Image>,
    val dashMpdUrl: String?,
    val hlsUrl: String?,
    val audioStreams: List<AudioStream>,
    val videoStreams: List<VideoStream>,
    val videoOnlyStreams: List<VideoStream>,
    val uploaderName: String,
    val uploaderUrl: String,
    val uploaderAvatars: List<Image>,
    val isUploaderVerified: Boolean,
    val subChannelName: String?,
    val subChannelUrl: String?,
    val subChannelAvatars: List<Image>,
    val relatedItems: List<InfoItem>,
    val nextStreams: List<InfoItem>,
    val uploadDate: OffsetDateTime?,
    val textualUploadDate: String?,
    val category: String,
    val tags: List<String>,
    val licence: String,
    val host: String,
    val privacy: StreamExtractor.Privacy
) {

    companion object {

        /**
         * Creates a [Streams] instance from a NewPipe [StreamInfo] object.
         *
         * This factory method converts a detailed NewPipe [StreamInfo] into a clean, immutable [Streams]
         * instance. It preserves all media stream variants, metadata, and related information while providing
         * a more idiomatic Kotlin API with improved naming conventions.
         *
         * The conversion includes:
         * - All metadata fields (title, description, duration, statistics)
         * - Complete media stream lists (audio, video, video-only)
         * - Adaptive streaming URLs (DASH and HLS)
         * - Uploader and channel information
         * - Related content and next stream recommendations
         * - Upload timing and categorization data
         * - Platform-specific details
         *
         * ## Implementation Notes
         * - The upload date is converted from NewPipe's DateWrapper to Java 8+ OffsetDateTime for better type safety
         * - All nullable fields are preserved as nullable in the Streams instance
         * - Collections are copied directly (both StreamInfo and Streams are intended to be immutable)
         *
         * @param streamInfo The source stream information from NewPipe extractor. Must not be null.
         * @return A populated [Streams] instance with all available metadata and stream variants.
         * All fields are guaranteed to be initialized, though some may be null or empty based on platform availability.
         *
         * @see StreamInfo for the source data structure
         */
        fun from(streamInfo: StreamInfo): Streams {
            return Streams(
                id = streamInfo.id,
                title = streamInfo.name,
                description = streamInfo.description,
                uploadDate = streamInfo.uploadDate?.offsetDateTime(),
                textualUploadDate = streamInfo.textualUploadDate,
                duration = streamInfo.duration,
                viewCount = streamInfo.viewCount,
                likeCount = streamInfo.likeCount,
                dislikeCount = streamInfo.dislikeCount,
                url = streamInfo.url,
                thumbnails = streamInfo.thumbnails,
                dashMpdUrl = streamInfo.dashMpdUrl,
                hlsUrl = streamInfo.hlsUrl,
                host = streamInfo.host,
                audioStreams = streamInfo.audioStreams,
                videoStreams = streamInfo.videoStreams,
                videoOnlyStreams = streamInfo.videoOnlyStreams,
                uploaderName = streamInfo.uploaderName,
                uploaderUrl = streamInfo.uploaderUrl,
                uploaderAvatars = streamInfo.uploaderAvatars,
                isUploaderVerified = streamInfo.isUploaderVerified,
                subChannelName = streamInfo.subChannelName,
                subChannelUrl = streamInfo.subChannelUrl,
                subChannelAvatars = streamInfo.subChannelAvatars,
                relatedItems = streamInfo.relatedItems,
                nextStreams = streamInfo.relatedStreams,
                category = streamInfo.category,
                tags = streamInfo.tags,
                licence = streamInfo.licence,
                privacy = streamInfo.privacy
            )
        }
    }
}