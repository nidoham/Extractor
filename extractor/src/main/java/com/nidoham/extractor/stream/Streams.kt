package com.nidoham.extractor.stream

import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.stream.StreamExtractor
import java.time.OffsetDateTime

/**
 * Immutable data class representing stream metadata and media streams.
 * Wrapper around [StreamInfo] with cleaner naming conventions.
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
         * Creates [Streams] from [StreamInfo].
         * @param streamInfo The source stream information
         * @return Populated [Streams] instance
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