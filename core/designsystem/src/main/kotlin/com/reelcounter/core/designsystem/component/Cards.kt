package com.reelcounter.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.reelcounter.core.designsystem.theme.Spacing

/**
 * The base card: a tonal surface with standard padding. Pass [onClick] to make the whole card one
 * tappable target. Either way, its content is read by screen readers as a single item.
 */
@Composable
fun RcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val inner = Modifier.padding(Spacing.lg)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, colors = colors) {
            Column(modifier = inner, content = content)
        }
    } else {
        Card(modifier = modifier.semantics(mergeDescendants = true) {}, colors = colors) {
            Column(modifier = inner, content = content)
        }
    }
}

/**
 * A single headline number with its label, e.g. "Total app time / 1 h 12 min".
 * [badge] sits under the value and is meant for a metric category tag.
 */
@Composable
fun RcStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onClick: (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    RcCard(modifier = modifier, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            badge?.invoke()
        }
    }
}
