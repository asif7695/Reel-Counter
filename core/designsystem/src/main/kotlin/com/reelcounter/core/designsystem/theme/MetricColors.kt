package com.reelcounter.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors for one metric category: a tonal [container], text on it, and a stronger [accent]. */
@Immutable
data class MetricColorRole(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
)

/**
 * Semantic colors for the four metric categories. They stay fixed under dynamic color, so
 * "Verified" and "Estimated" look the same on every device.
 *
 * Color never carries meaning alone: every badge pairs it with an icon and a text label.
 */
@Immutable
data class MetricColors(
    val verified: MetricColorRole,
    val estimated: MetricColorRole,
    val durationOnly: MetricColorRole,
    val unavailable: MetricColorRole,
)

internal val LightMetricColors = MetricColors(
    verified = MetricColorRole(
        container = Color(0xFFC3EFCB),
        onContainer = Color(0xFF00210C),
        accent = Color(0xFF1B6D3A),
    ),
    estimated = MetricColorRole(
        container = Color(0xFFFFDEA6),
        onContainer = Color(0xFF271900),
        accent = Color(0xFF7C5800),
    ),
    durationOnly = MetricColorRole(
        container = Color(0xFFD7E3F8),
        onContainer = Color(0xFF101C2B),
        accent = Color(0xFF535F70),
    ),
    // Replaced at runtime with the active scheme's neutrals; see ReelCounterTheme.
    unavailable = MetricColorRole(
        container = Color(0xFFE4E1E9),
        onContainer = Color(0xFF46464F),
        accent = Color(0xFF767680),
    ),
)

internal val DarkMetricColors = MetricColors(
    verified = MetricColorRole(
        container = Color(0xFF005228),
        onContainer = Color(0xFFA3F5B4),
        accent = Color(0xFF88D89A),
    ),
    estimated = MetricColorRole(
        container = Color(0xFF5E4200),
        onContainer = Color(0xFFFFDEA6),
        accent = Color(0xFFF2BF48),
    ),
    durationOnly = MetricColorRole(
        container = Color(0xFF3B4858),
        onContainer = Color(0xFFD7E3F8),
        accent = Color(0xFFBBC7DB),
    ),
    unavailable = MetricColorRole(
        container = Color(0xFF35343A),
        onContainer = Color(0xFFC7C5D0),
        accent = Color(0xFF91909A),
    ),
)

val LocalMetricColors = staticCompositionLocalOf { LightMetricColors }
