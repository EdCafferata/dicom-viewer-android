package info.cafferata.dicomviewer.util

import android.text.format.DateUtils

/** Relative time ("3 days ago", "3 dagen geleden", …) in the device language. */
fun Long.timeAgo(): String =
    DateUtils.getRelativeTimeSpanString(this, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
