package com.reelcounter.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.reelcounter.core.designsystem.component.RcCard
import com.reelcounter.core.designsystem.component.RcTopAppBar
import com.reelcounter.core.designsystem.icon.RcIcons
import com.reelcounter.core.designsystem.theme.Spacing
import com.reelcounter.core.model.MetricQuality
import com.reelcounter.core.ui.metric.MetricQualityBadge

/** Plain-language explanation of the four metric categories, reachable from Settings and, later, from every badge. */
@Composable
internal fun HowWeCountScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            RcTopAppBar(
                title = stringResource(R.string.feature_settings_how_we_count),
                navigationIcon = RcIcons.Back,
                navigationIconContentDescription = stringResource(R.string.feature_settings_back),
                onNavigationClick = onBackClick,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.feature_settings_how_we_count_intro),
                style = MaterialTheme.typography.bodyLarge,
            )
            MetricQuality.entries.forEach { quality ->
                RcCard(modifier = Modifier.fillMaxWidth()) {
                    MetricQualityBadge(quality = quality)
                    Text(
                        text = stringResource(quality.descriptionRes),
                        modifier = Modifier.padding(top = Spacing.sm),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                text = stringResource(R.string.feature_settings_app_time_title),
                modifier = Modifier
                    .padding(top = Spacing.sm)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.feature_settings_app_time_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@get:StringRes
private val MetricQuality.descriptionRes: Int
    get() = when (this) {
        MetricQuality.VERIFIED -> R.string.feature_settings_quality_verified
        MetricQuality.ESTIMATED -> R.string.feature_settings_quality_estimated
        MetricQuality.DURATION_ONLY -> R.string.feature_settings_quality_duration_only
        MetricQuality.UNAVAILABLE -> R.string.feature_settings_quality_unavailable
    }
