package com.example.transportapp.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.PlexMonoFamily
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import com.example.transportapp.core.designsystem.theme.transportColors
import com.example.transportapp.domain.transport.ConsignmentStatus
import com.example.transportapp.domain.transport.PaymentMode

/**
 * Family 1 — Journey chips (Design.md §A8). Eleven fixed wordings, no others.
 * Filled pill, 24dp tall, labelMedium.
 */
@Composable
fun JourneyChip(
    status: ConsignmentStatus,
    modifier: Modifier = Modifier
) {
    val colors = when (status) {
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

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 100))
            .then(
                if (colors.bordered) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
                } else {
                    Modifier.background(colors.container)
                }
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (status) {
            ConsignmentStatus.DELIVERED -> Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.label
            )
            ConsignmentStatus.HELD, ConsignmentStatus.RETURNED -> Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.label
            )
            else -> Unit
        }
        Text(
            text = status.wording,
            style = TransportTypeScale.labelMedium,
            color = colors.label,
            maxLines = 1
        )
    }
}

/**
 * Family 2 — The payment stamp (Design.md §A8). Not a chip: a 4dp-radius rectangle,
 * 2dp border, no fill, mono 12dp caps tracked 1.2, rotated 3° anticlockwise.
 */
@Composable
fun PaymentStamp(
    mode: PaymentMode,
    modifier: Modifier = Modifier,
    onPaper: Boolean = false,
    contentDescription: String = "Payment mode: ${mode.label}"
) {
    val color = when {
        onPaper -> transportColors().stampViolet
        mode == PaymentMode.PAID -> MaterialTheme.colorScheme.primary
        else -> transportColors().haulAmber
    }
    Box(
        modifier = modifier
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
 * Family 3 — Sync chips (Design.md §A8). 20dp tall. "Synced" is hidden by default
 * everywhere except the sync queue on T31.
 */
enum class SyncState(val wording: String) { PENDING("Pending sync"), SYNCING("Syncing"), SYNCED("Synced") }

@Composable
fun SyncChip(
    state: SyncState,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false
) {
    val amberContainer = transportColors().haulAmberContainer
    val amber = transportColors().haulAmber
    val labelColor = transportColors().onHaulAmber
    when (state) {
        SyncState.PENDING -> Row(
            modifier = modifier
                .clip(RoundedCornerShape(percent = 100))
                .background(amberContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(labelColor)
            )
            Text(state.wording, style = TransportTypeScale.labelMedium, color = labelColor, maxLines = 1)
        }
        SyncState.SYNCING -> Row(
            modifier = modifier
                .clip(RoundedCornerShape(percent = 100))
                .background(amberContainer)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(labelColor.copy(alpha = if (pulsing) 0.6f else 1f))
            )
            Text(state.wording, style = TransportTypeScale.labelMedium, color = labelColor, maxLines = 1)
        }
        SyncState.SYNCED -> Row(
            modifier = modifier
                .clip(RoundedCornerShape(percent = 100))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
 * Selected: filled secondaryContainer / onSecondaryContainer. Unselected: 1dp outline border.
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
    val container = if (selected) {
        Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(percent = 100))
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(percent = 100))
    }
    val labelColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .then(container)
            .clip(RoundedCornerShape(percent = 100))
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
 * Segmented button control (Design.md §B3): row of 40dp pills, selected in
 * secondaryContainer with onSecondaryContainer label, unselected outlined.
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
 * A group heading — titleSmall onSurfaceVariant in tracked caps (Design.md §B5).
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
            text = text.uppercase(),
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