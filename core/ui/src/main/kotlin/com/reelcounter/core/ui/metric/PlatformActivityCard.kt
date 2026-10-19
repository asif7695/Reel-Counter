package com.reelcounter.core.ui.metric

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.reelcounter.core.designsystem.component.RcCard
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.designsystem.theme.withTabularNumbers
import com.reelcounter.core.model.DailyPlatformActivity
import com.reelcounter.core.model.PlatformMetric
import com.reelcounter.core.model.UnavailableReason
import com.reelcounter.core.ui.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * One platform's day. The card follows the display rules from the Phase 1 docs:
 * - an estimate always shows "≈" and its range;
 * - duration is labeled "app time", never "reels time";
 * - a duration-only day shows no count;
 * - every figure carries its category badge.
 */
@Composable
fun PlatformActivityCard(
    activity: DailyPlatformActivity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    val metric = activity.metric

    RcCard(modifier = modifier, onClick = onClick) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            PlatformMonogram(name = activity.platform.displayName)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = activity.platform.displayName,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = headline(metric, locale),
                    style = MaterialTheme.typography.headlineSmall.withTabularNumbers(),
                )
                supportingText(metric, locale)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MetricQualityBadge(
                    quality = metric.quality,
                    userAdjusted = (metric as? PlatformMetric.Estimated)?.userAdjusted == true,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}

@Composable
private fun headline(metric: PlatformMetric, locale: Locale): String = when (metric) {
    is PlatformMetric.Verified -> pluralStringResource(
        R.plurals.core_ui_metric_verified_count,
        metric.count,
        formatCount(metric.count, locale),
    )
    is PlatformMetric.Estimated -> pluralStringResource(
        R.plurals.core_ui_metric_estimated_count,
        metric.count,
        formatCount(metric.count, locale),
    )
    is PlatformMetric.DurationOnly -> stringResource(
        R.string.core_ui_metric_app_time,
        formatAppTime(metric.appTime),
    )
    is PlatformMetric.Unavailable -> stringResource(R.string.core_ui_metric_unavailable)
}

@Composable
private fun supportingText(metric: PlatformMetric, locale: Locale): String? = when (metric) {
    is PlatformMetric.Verified -> {
        val through = stringResource(R.string.core_ui_metric_verified_through, formatDate(metric.verifiedThrough, locale))
        metric.appTime?.let { appTime ->
            stringResource(
                R.string.core_ui_metric_joined,
                through,
                stringResource(R.string.core_ui_metric_app_time, formatAppTime(appTime)),
            )
        } ?: through
    }
    is PlatformMetric.Estimated -> stringResource(
        R.string.core_ui_metric_joined,
        stringResource(
            R.string.core_ui_metric_estimate_range,
            formatCount(metric.low, locale),
            formatCount(metric.high, locale),
        ),
        stringResource(R.string.core_ui_metric_app_time, formatAppTime(metric.appTime)),
    )
    is PlatformMetric.DurationOnly -> stringResource(R.string.core_ui_metric_no_count_explainer)
    is PlatformMetric.Unavailable -> stringResource(metric.reason.messageRes)
}

/** A letter avatar instead of the platform's logo, which the app doesn't use (trademark rules). */
@Composable
private fun PlatformMonogram(name: String) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private val UnavailableReason.messageRes: Int
    get() = when (this) {
        UnavailableReason.PERMISSION_MISSING -> R.string.core_ui_unavailable_permission_missing
        UnavailableReason.TRACKING_PAUSED -> R.string.core_ui_unavailable_tracking_paused
        UnavailableReason.APP_NOT_INSTALLED -> R.string.core_ui_unavailable_app_not_installed
        UnavailableReason.COVERAGE_GAP -> R.string.core_ui_unavailable_coverage_gap
        UnavailableReason.NOT_OBSERVABLE -> R.string.core_ui_unavailable_not_observable
    }

@Composable
private fun formatCount(count: Int, locale: Locale): String =
    remember(count, locale) { NumberFormat.getIntegerInstance(locale).format(count) }

@Composable
private fun formatDate(date: LocalDate, locale: Locale): String =
    remember(date, locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(date)
    }
