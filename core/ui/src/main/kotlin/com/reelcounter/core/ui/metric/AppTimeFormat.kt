package com.reelcounter.core.ui.metric

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.reelcounter.core.ui.R
import kotlin.time.Duration

/** App time split for display. Under a minute (but above zero) is shown as "<1 min", not "0 min". */
internal data class AppTimeParts(val hours: Long, val minutes: Long, val isUnderOneMinute: Boolean)

internal fun appTimeParts(appTime: Duration): AppTimeParts {
    val totalMinutes = appTime.inWholeMinutes
    return AppTimeParts(
        hours = totalMinutes / 60,
        minutes = totalMinutes % 60,
        isUnderOneMinute = totalMinutes == 0L && appTime.isPositive(),
    )
}

/** Short app-time text such as "1 h 12 min", "45 min" or "<1 min". */
@Composable
fun formatAppTime(appTime: Duration): String {
    val parts = appTimeParts(appTime)
    return when {
        parts.isUnderOneMinute -> stringResource(R.string.core_ui_duration_under_minute)
        parts.hours == 0L -> stringResource(R.string.core_ui_duration_minutes, parts.minutes)
        parts.minutes == 0L -> stringResource(R.string.core_ui_duration_hours, parts.hours)
        else -> stringResource(R.string.core_ui_duration_hours_minutes, parts.hours, parts.minutes)
    }
}
