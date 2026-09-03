package com.example.transportapp.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalShipping
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.Dimens
import com.example.transportapp.core.designsystem.theme.HaulMotion
import com.example.transportapp.core.designsystem.theme.TransportTypeScale
import kotlin.math.roundToInt

/** RouteLine step state. */
enum class StepState { DONE, CURRENT, UPCOMING }

/** A single step on the route line. */
data class RouteLineStep(
    val label: String,
    val state: StepState,
    val secondary: String? = null
)

/** Orientation of the route line. */
enum class RouteLineOrientation { HORIZONTAL, VERTICAL }

/**
 * The route line — the single signature primitive (Design.md §A9.2).
 *
 * S20 (D57) — the Night Haul Expressive rebuild:
 *  - the **truck is really placed** on the current tick (B1's dead block is gone) and
 *    *drives* there with a spring on first composition;
 *  - the travelled segment **draws itself** with the emphasized easing;
 *  - the truck idles with a gentle bob while the consignment is in motion (CURRENT).
 */
@Composable
fun RouteLine(
    steps: List<RouteLineStep>,
    modifier: Modifier = Modifier,
    orientation: RouteLineOrientation = RouteLineOrientation.HORIZONTAL,
    showTruck: Boolean = true,
    showLabels: Boolean = true,
    currentIndex: Int = steps.indexOfFirst { it.state == StepState.CURRENT }.let { if (it < 0) 0 else it }
) {
    when (orientation) {
        RouteLineOrientation.HORIZONTAL -> HorizontalRouteLine(
            steps = steps,
            showTruck = showTruck,
            showLabels = showLabels,
            currentIndex = currentIndex,
            modifier = modifier
        )
        RouteLineOrientation.VERTICAL -> VerticalRouteLine(
            steps = steps,
            currentIndex = currentIndex,
            modifier = modifier
        )
    }
}

@Composable
private fun HorizontalRouteLine(
    steps: List<RouteLineStep>,
    showTruck: Boolean,
    showLabels: Boolean,
    currentIndex: Int,
    modifier: Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // The travelled fraction animates from 0 — the road draws itself on open (S20).
    val targetFraction = if (steps.size > 1) currentIndex.toFloat() / (steps.size - 1) else 0f
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fraction by animateFloatAsState(
        targetValue = if (started) targetFraction else 0f,
        animationSpec = HaulMotion.enterFloat(),
        label = "routeDraw",
    )

    // Gentle idle bob while the consignment is in motion.
    val inMotion = steps.getOrNull(currentIndex)?.state == StepState.CURRENT
    val bob = if (inMotion && showTruck) {
        val idle = rememberInfiniteTransition(label = "truckIdle")
        val v by idle.animateFloat(0f, -3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "truckBob")
        v
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.routeTruckSize + 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val density = LocalDensity.current
            val truckSize = Dimens.routeTruckSize
            val truckSizePx = with(density) { truckSize.toPx() }
            val maxX = with(density) { maxWidth.toPx() } - truckSizePx
            val truckOffset = IntOffset((maxX * fraction).roundToInt(), bob.roundToInt())

            Canvas(modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.CenterStart)) {
                val lineY = size.height / 2
                drawLine(outlineVariant, Offset(0f, lineY), Offset(size.width, lineY), strokeWidth = 2.dp.toPx())
                if (fraction > 0f) {
                    drawLine(primary, Offset(0f, lineY), Offset(size.width * fraction, lineY), strokeWidth = 2.dp.toPx())
                }
                steps.forEachIndexed { i, step ->
                    val x = if (steps.size > 1) size.width * i / (steps.size - 1) else size.width / 2
                    when (step.state) {
                        StepState.DONE -> drawCircle(primary, radius = 4.dp.toPx(), center = Offset(x, lineY))
                        StepState.CURRENT -> {
                            drawCircle(primary.copy(alpha = 0.12f), radius = 10.dp.toPx(), center = Offset(x, lineY))
                            drawCircle(primary, radius = 4.dp.toPx(), center = Offset(x, lineY))
                        }
                        StepState.UPCOMING -> {
                            drawCircle(surface, radius = 4.dp.toPx(), center = Offset(x, lineY))
                            drawCircle(outlineVariant, radius = 4.dp.toPx(), center = Offset(x, lineY), style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
            }
            // The truck really stands on the current tick (B1 closed) and drove there (S20).
            if (showTruck && steps.isNotEmpty()) {
                Icon(
                    Icons.Rounded.LocalShipping,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset { IntOffset((maxX * fraction).roundToInt(), bob.roundToInt()) }
                        .size(truckSize)
                )
            }
        }
        // Tick labels
        if (showLabels && steps.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                steps.forEachIndexed { i, step ->
                    Text(
                        text = step.label,
                        style = TransportTypeScale.labelMedium,
                        color = if (step.state == StepState.UPCOMING) onSurfaceVariant else onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalRouteLine(
    steps: List<RouteLineStep>,
    currentIndex: Int,
    modifier: Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        steps.forEachIndexed { i, step ->
            val isLast = i == steps.size - 1
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier.width(24.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(
                        modifier = Modifier.width(2.dp).height(if (isLast) 16.dp else 32.dp)
                    ) {
                        drawLine(
                            color = if (step.state == StepState.UPCOMING) outlineVariant else primary,
                            start = Offset(size.width / 2, 0f),
                            end = Offset(size.width / 2, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                    Box(
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        when (step.state) {
                            StepState.DONE -> Canvas(
                                modifier = Modifier.width(8.dp).height(8.dp)
                            ) {
                                drawCircle(primary, radius = 4.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
                            }
                            StepState.CURRENT -> {
                                Canvas(
                                    modifier = Modifier.width(20.dp).height(20.dp)
                                ) {
                                    val center = Offset(size.width / 2, size.height / 2)
                                    drawCircle(primary.copy(alpha = 0.12f), radius = 10.dp.toPx(), center = center)
                                    drawCircle(primary, radius = 4.dp.toPx(), center = center)
                                }
                                if (i == currentIndex) {
                                    Icon(
                                        Icons.Rounded.LocalShipping,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(20.dp)
                                    )
                                }
                            }
                            StepState.UPCOMING -> Canvas(
                                modifier = Modifier.width(8.dp).height(8.dp)
                            ) {
                                val center = Offset(size.width / 2, size.height / 2)
                                drawCircle(surface, radius = 4.dp.toPx(), center = center)
                                drawCircle(outlineVariant, radius = 4.dp.toPx(), center = center, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Text(
                        text = step.label,
                        style = TransportTypeScale.bodyMedium,
                        color = onSurface,
                        maxLines = 1
                    )
                    if (step.secondary != null) {
                        Text(
                            text = step.secondary,
                            style = TransportTypeScale.bodySmall,
                            color = onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact route line for the Vehicle board card. S20: the travelled segment draws
 * itself and the truck rides the current position (placed by width fraction).
 */
@Composable
fun CompactRouteLine(
    stopCount: Int,
    currentPosition: Int,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val target = if (stopCount > 1) currentPosition.toFloat() / (stopCount - 1) else 0f
    var started by remember { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { started = true }
    val fraction by animateFloatAsState(
        targetValue = if (started) target else 0f,
        animationSpec = HaulMotion.enterFloat(),
        label = "compactRouteDraw",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val truckSize = 12.dp
        val maxX = with(density) { maxWidth.toPx() } - with(density) { truckSize.toPx() }

        Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            val lineY = size.height / 2
            drawLine(outlineVariant, Offset(0f, lineY), Offset(size.width, lineY), strokeWidth = 2.dp.toPx())
            if (fraction > 0) {
                drawLine(primary, Offset(0f, lineY), Offset(size.width * fraction, lineY), strokeWidth = 2.dp.toPx())
            }
            val tickX = if (stopCount > 1) size.width * currentPosition / (stopCount - 1) else size.width / 2
            drawCircle(primary, radius = 4.dp.toPx(), center = Offset(tickX, lineY))
        }
        if (currentPosition > 0) {
            Icon(
                Icons.Rounded.LocalShipping,
                contentDescription = null,
                tint = primary,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((maxX * fraction).roundToInt(), 0) }
                    .size(truckSize)
            )
        }
    }
}
