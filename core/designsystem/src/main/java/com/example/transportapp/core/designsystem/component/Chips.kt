package com.example.transportapp.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode

/**
 * Journey chips. Eleven fixed wordings, no others.
 * Filled pill, 24dp tall, labelMedium. Animated color transition on state change.
 */
@Composable
fun JourneyChip(
    status: ConsignmentStatus,
    modifier: Modifier = Modifier
) {
    val chipColors = when (status) {
        ConsignmentStatus.DRAFT, ConsignmentStatus.BOOKED, ConsignmentStatus.LOADED ->
            ChipColors(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant)
        ConsignmentStatus.IN_TRANSIT, ConsignmentStatus.AT_HUB, ConsignmentStatus.ARRIVED,
        ConsignmentStatus.OUT_FOR_DELIVERY ->
            ChipColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        ConsignmentStatus.DELIVERED ->
            ChipColors(transportColors().deliveredContainer, MaterialTheme.colorScheme.onPrimaryContainer)
        ConsignmentStatus.HELD, ConsignmentStatus.RETURNED ->
            ChipColors(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        ConsignmentStatus.CANCELLED ->
            ChipColors(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant, bordered = true)
    }

    val animatedContainer by animateColorAsState(
        targetValue = chipColors.container,
        animationSpec = HaulMotion.short(),
        label = "chipContainer",
    )
    val animatedLabel by animateColorAsState(
        targetValue = chipColors.label,
        animationSpec = HaulMotion.short(),
        label = "chipLabel",
    )

    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(percent = 100))
            .then(
                if (chipColors.bordered) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
                } else {
                    Modifier.background(animatedContainer)
                }
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (status) {
            ConsignmentStatus.DELIVERED -> Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = animatedLabel
            )
            ConsignmentStatus.HELD, ConsignmentStatus.RETURNED -> Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = animatedLabel
            )
            else -> Unit
        }
        Text(
            text = status.wording,
            style = TransportTypeScale.labelMedium,
            color = animatedLabel,
            maxLines = 1
        )
    }
}

/**
 * S21 — the shared filter bottom sheet: a title, a column of option rows, and a Done
 * action. The Tune icon on list screens opens this; the options are the screen's own
 * existing filter events, consolidated in one place instead of a dead icon.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = AppShapesSheet.sheet,
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = TransportTypeScale.titleLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text("Done", color = MaterialTheme.colorScheme.primary)
                }
            }
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Sheet shape accessor kept here to avoid a theme circular import in this file. */
private object AppShapesSheet {
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}

/**
 * Family 2 — The payment stamp (Design.md §A8). Not a chip: a 4dp-radius rectangle,
 * 2dp border, no fill, mono 12dp caps tracked 1.2, rotated 3° anticlockwise.
 * S20 (D57): the stamp *lands* — scales up from 0.6 with a bouncy spring and settles
 * at 3° the first time it appears (Design.md T5's V5 moment).
 */
@Composable
fun PaymentStamp(
    mode: PaymentMode,
    modifier: Modifier = Modifier,
    onPaper: Boolean = false,
    contentDescription: String = "Payment mode: ${mode.label}"
) {
    val color by animateColorAsState(
        targetValue = when {
            onPaper -> transportColors().stampViolet
            mode == PaymentMode.PAID -> MaterialTheme.colorScheme.primary
            else -> transportColors().haulAmber
        },
        animationSpec = HaulMotion.short(),
        label = "stampColor",
    )
    var landed by remember(mode) { mutableStateOf(false) }
    LaunchedEffect(mode) { landed = true }
    val scale by animateFloatAsState(
        targetValue = if (landed) 1f else 0.6f,
        animationSpec = HaulMotion.bouncy,
        label = "stampLand",
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .rotate(-3f)
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text = mode.stampText,
            color = color,
            fontFamily = PlexMonoFamily,
            fontSize = 12.sp,
            letterSpacing = 1.2.sp,
            maxLines = 1
        )
    }
}

/**
 * Sync chips. 20dp tall. "Synced" is hidden by default everywhere except the sync queue.
 */
enum class SyncState(val wording: String) { PENDING("Pending sync"), SYNCING("Syncing"), SYNCED("Synced") }

@Composable
fun SyncChip(
    state: SyncState,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false
) {
    val amberContainer = transportColors().haulAmberContainer
    val labelColor = transportColors().onHaulAmber
    val dotAlpha by animateFloatAsState(
        targetValue = if (state == SyncState.SYNCING && pulsing) 0.5f else 1f,
        animationSpec = HaulMotion.utilFloat(),
        label = "syncDot",
    )
    when (state) {
        SyncState.PENDING -> Row(
            modifier = modifier
                .height(24.dp)
                .clip(RoundedCornerShape(percent = 100))
                .background(amberContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(labelColor)
            )
            Text(state.wording, style = TransportTypeScale.labelMedium, color = labelColor, maxLines = 1)
        }
        SyncState.SYNCING -> Row(
            modifier = modifier
                .height(24.dp)
                .clip(RoundedCornerShape(percent = 100))
                .background(amberContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(labelColor.copy(alpha = dotAlpha))
            )
            Text(state.wording, style = TransportTypeScale.labelMedium, color = labelColor, maxLines = 1)
        }
        SyncState.SYNCED -> Row(
            modifier = modifier
                .height(24.dp)
                .clip(RoundedCornerShape(percent = 100))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(state.wording, style = TransportTypeScale.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

private data class ChipColors(
    val container: Color,
    val label: Color,
    val bordered: Boolean = false
)

/**
 * Filter chip row used across screens (Register, Board, Unbilled, ...).
 * Selected: filled secondaryContainer / onSecondaryContainer with animated transition.
 * Unselected: 1dp outline border. Both get press-scale feedback.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    height: Dp = Dimens.filterChipHeight
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = HaulMotion.short(),
        label = "filterBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = HaulMotion.short(),
        label = "filterBorder",
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = HaulMotion.short(),
        label = "filterLabel",
    )
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(percent = 100))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(percent = 100))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = TransportTypeScale.labelMedium, color = labelColor, maxLines = 1)
        trailingIcon?.invoke()
    }
}

/**
 * Segmented button control: row of 40dp pills, selected in secondaryContainer
 * with onSecondaryContainer label, unselected outlined.
 */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    containerHeight: Dp = Dimens.segmentedButtonHeight
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                label = label,
                selected = value == selected,
                onClick = { onSelect(value) },
                height = containerHeight
            )
        }
    }
}

/**
 * A group heading — titleSmall onSurfaceVariant in tracked caps.
 */
@Composable
fun GroupHeading(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = text,
            style = TransportTypeScale.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke(this)
    }
}

/** Centered caption used under labels (e.g. "Or choose another"). */
@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = TransportTypeScale.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        modifier = modifier
    )
}
