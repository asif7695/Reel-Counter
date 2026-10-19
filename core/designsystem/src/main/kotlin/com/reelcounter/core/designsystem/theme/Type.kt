package com.reelcounter.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val Base = Typography()

/**
 * The Material 3 type scale on the system font, with heavier headings for hierarchy.
 * Display styles use tabular figures so counts don't shift width as they change.
 */
internal val RcTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontWeight = FontWeight.SemiBold).withTabularNumbers(),
    displayMedium = Base.displayMedium.copy(fontWeight = FontWeight.SemiBold).withTabularNumbers(),
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.SemiBold).withTabularNumbers(),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall,
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    bodySmall = Base.bodySmall,
    labelLarge = Base.labelLarge,
    labelMedium = Base.labelMedium,
    labelSmall = Base.labelSmall,
)

/** Fixed-width digits, for numbers that update in place or line up in columns. */
fun TextStyle.withTabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")
