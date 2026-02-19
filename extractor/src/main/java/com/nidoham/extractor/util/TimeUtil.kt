package com.nidoham.extractor.util

import java.util.Locale

object TimeUtil {

    fun Long.formatDuration(): String = when {
        this <= 0 -> "LIVE"
        this >= 3600 -> String.format(Locale.US, "%d:%02d:%02d", this / 3600, (this % 3600) / 60, this % 60)
        else -> String.format(Locale.US, "%d:%02d", this / 60, this % 60)
    }

    fun Long.formatCount(): String = when {
        this >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", this / 1_000_000_000.0)
        this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format(Locale.US, "%.1fK", this / 1_000.0)
        else -> this.toString()
    }
}