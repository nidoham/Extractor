package com.nidoham.extractor.util

import java.util.Date
import java.util.Locale

object TimeUtil {

    /**
     * Formats a duration in seconds to a human-readable timestamp string.
     *
     * Returns an empty string for durations <= 0 so the UI simply omits the
     * badge. The old "LIVE" fallback has been removed — live streams are now
     * identified by [StreamItem.isLive] and rendered with a dedicated badge.
     *
     * Examples:
     *   3661 → "1:01:01"
     *    90  → "1:30"
     *     5  → "0:05"
     *    -1  → ""
     */
    fun Long.formatDuration(): String = when {
        this <= 0L  -> ""
        this >= 3600L -> String.format(
            Locale.US, "%d:%02d:%02d",
            this / 3600L,
            (this % 3600L) / 60L,
            this % 60L
        )
        else -> String.format(
            Locale.US, "%d:%02d",
            this / 60L,
            this % 60L
        )
    }

    /**
     * Formats a large count to a compact human-readable string.
     *
     * Examples:
     *   1_500_000_000 → "1.5B"
     *       2_300_000 → "2.3M"
     *          45_000 → "45.0K"
     *             999 → "999"
     */
    fun Long.formatCount(): String = when {
        this >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", this / 1_000_000_000.0)
        this >= 1_000_000L     -> String.format(Locale.US, "%.1fM", this / 1_000_000.0)
        this >= 1_000L         -> String.format(Locale.US, "%.1fK", this / 1_000.0)
        else                   -> this.toString()
    }

    /**
     * Returns a YouTube-style relative time string for this [Date].
     *
     * Examples:
     *   2 years ago · 3 months ago · 1 week ago · 4 days ago
     *   5 hours ago · 30 minutes ago · Just now
     *
     * Usage:
     *   item.uploadDate?.date()?.formatRelativeTime()
     */
    fun Date.formatRelativeTime(): String {
        val diffMs  = System.currentTimeMillis() - time
        val seconds = diffMs / 1_000L
        val minutes = seconds / 60L
        val hours   = minutes / 60L
        val days    = hours   / 24L
        val weeks   = days    / 7L
        val months  = days    / 30L
        val years   = days    / 365L

        return when {
            years   > 0L -> "$years ${if (years   == 1L) "year"   else "years"}   ago"
            months  > 0L -> "$months ${if (months == 1L) "month"  else "months"}  ago"
            weeks   > 0L -> "$weeks ${if (weeks   == 1L) "week"   else "weeks"}   ago"
            days    > 0L -> "$days ${if (days     == 1L) "day"    else "days"}    ago"
            hours   > 0L -> "$hours ${if (hours   == 1L) "hour"   else "hours"}   ago"
            minutes > 0L -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            else         -> "Just now"
        }
    }
}