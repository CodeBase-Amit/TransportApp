package com.example.transportapp.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors

private val Pill = RoundedCornerShape(percent = 100)

/** The shared press-scale — the button answers the finger with a spring. */
@Composable
private fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = HaulMotion.press,
        label = "pressScale",
    )
    return this.scale(scale)
}

/**
 * Primary filled pill button: primary fill, 56dp tall, pill.
 * Spring press-scale + a soft primary glow; `celebrate` swaps to the sunrise
 * accent for the app's peak moments (booking saved, payment collected).
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = Dimens.primaryButtonHeight,
    celebrate: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val glow = transportColors().shadowTint
    val container by animateColorAsState(
        targetValue = if (celebrate) transportColors().sunrise else MaterialTheme.colorScheme.primary,
        animationSpec = HaulMotion.util(),
        label = "btnContainer",
    )
    val content by animateColorAsState(
        targetValue = if (celebrate) transportColors().onSunrise else MaterialTheme.colorScheme.onPrimary,
        animationSpec = HaulMotion.util(),
        label = "btnContent",
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .pressScale(interaction)
            .softGlow(if (enabled) glow else Color.Transparent),
        enabled = enabled,
        shape = Pill,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = TransportTypeScale.labelLarge)
    }
}

/** A soft tinted glow behind a control — always the app's own shadow tint. */
fun Modifier.softGlow(color: Color): Modifier = this.shadow(
    elevation = 12.dp,
    shape = Pill,
    ambientColor = color,
    spotColor = color,
)

/** Filled tonal pill button — secondaryContainer fill with press-scale feedback. */
@Composable
fun AppTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = Dimens.primaryButtonHeight
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .pressScale(interaction),
        enabled = enabled,
        shape = Pill,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = TransportTypeScale.labelLarge)
    }
}

/** Outlined pill button — 1dp outline border, primary label with press feedback. */
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
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .pressScale(interaction),
        enabled = enabled,
        shape = Pill,
        interactionSource = interaction,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            contentColor = labelColor,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        )
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = TransportTypeScale.labelLarge)
    }
}

/** Text button — primary label, no container with press feedback. */
@Composable
fun AppTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(interaction),
        enabled = enabled,
        interactionSource = interaction,
        colors = ButtonDefaults.textButtonColors(contentColor = color)
    ) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = TransportTypeScale.labelLarge)
    }
}

/** Destructive outlined pill button — error border/label with press feedback. */
@Composable
fun AppDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    filled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier
                .height(height)
                .pressScale(interaction),
            shape = Pill,
            interactionSource = interaction,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
            )
        ) {
            Text(text, style = TransportTypeScale.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .height(height)
                .pressScale(interaction),
            shape = Pill,
            interactionSource = interaction,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.buttonColors(
                contentColor = MaterialTheme.colorScheme.error,
                disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
            )
        ) {
            Text(text, style = TransportTypeScale.labelLarge)
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
    val interaction = remember { MutableInteractionSource() }
    TextButton(onClick = onClick, modifier = modifier.pressScale(interaction), interactionSource = interaction) {
        if (leadingIcon != null) Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(text, style = TransportTypeScale.labelLarge, color = color)
    }
}