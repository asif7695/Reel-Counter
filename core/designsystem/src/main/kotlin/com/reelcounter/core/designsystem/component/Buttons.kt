package com.reelcounter.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.reelcounter.core.designsystem.theme.Spacing

/**
 * Primary action. While [loading] is true the button shows a spinner and ignores taps, so an
 * action can't be submitted twice.
 */
@Composable
fun RcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = Spacing.minTouchTarget),
        enabled = enabled && !loading,
        contentPadding = contentPaddingFor(leadingIcon, loading),
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon, loading = loading)
    }
}

/** Secondary action, shown alongside an [RcButton]. */
@Composable
fun RcOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Spacing.minTouchTarget),
        enabled = enabled && !loading,
        contentPadding = contentPaddingFor(leadingIcon, loading),
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon, loading = loading)
    }
}

/** Low-emphasis action, for example inside cards and dialogs. */
@Composable
fun RcTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = Spacing.minTouchTarget),
        enabled = enabled,
        contentPadding = if (leadingIcon != null) {
            ButtonDefaults.TextButtonWithIconContentPadding
        } else {
            ButtonDefaults.TextButtonContentPadding
        },
    ) {
        ButtonContent(text = text, leadingIcon = leadingIcon, loading = false)
    }
}

private fun contentPaddingFor(leadingIcon: ImageVector?, loading: Boolean) =
    if (leadingIcon != null || loading) {
        ButtonDefaults.ButtonWithIconContentPadding
    } else {
        ButtonDefaults.ContentPadding
    }

@Composable
private fun RowScope.ButtonContent(text: String, leadingIcon: ImageVector?, loading: Boolean) {
    when {
        loading -> CircularProgressIndicator(
            modifier = Modifier.size(ButtonDefaults.IconSize),
            color = LocalContentColor.current,
            strokeWidth = 2.dp,
        )
        leadingIcon != null -> Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
    }
    if (loading || leadingIcon != null) {
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    }
    Text(text = text)
}
