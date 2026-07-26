package info.cafferata.dicomviewer.util

import java.util.concurrent.TimeUnit

fun Long.timeAgo(): String {
    val diffMs = System.currentTimeMillis() - this
    if (diffMs < 0) return "zojuist"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        minutes < 1 -> "zojuist"
        minutes < 60 -> "${minutes}m geleden"
        hours < 24 -> "${hours}u geleden"
        days < 7 -> "${days}d geleden"
        else -> "${days / 7}w geleden"
    }
}
