package com.example.transportapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens

private val Pill = RoundedCornerShape(percent = 100)

/**
 * Primary filled pill button (Design.md §B3): primary fill, 56dp tall, pill.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = Dimens.primaryButtonHeight
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.height(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Filled tonal pill button — secondaryContainer fill. */
@Composable
fun AppTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = Dimens.primaryButtonHeight
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = Pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.height(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Outlined pill button — 1dp outline border, primary label. */
@Composable
fun AppOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = Dimens.primaryButtonHeight,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    labelColor: Color = MaterialTheme.colorScheme.primary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = Pill,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            contentColor = labelColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.height(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Text button — primary label, no container. */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.height(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Destructive outlined pill button — error border/label. */
@Composable
fun AppDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    filled: Boolean = true
) {
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier.height(height),
            shape = Pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(height),
            shape = Pill,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.buttonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Text button row used at the foot of lists and cards. */
@Composable
fun AppListTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    TextButton(onClick = onClick, modifier = modifier) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.height(20.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}