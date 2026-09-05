package com.example.transportapp.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.transportapp.core.designsystem.theme.PaperColors

/**
 * Finger signature pad (Design.md §C1 T33). A paper-coloured signing area with a baseline
 * hairline; strokes are captured as a [Path]. Used for the POD signature.
 *
 * The canvas clears itself whenever [clearSignal] changes. [onPathChange] reports the live
 * stroke so the caller can export it (S15: rendered to a PNG for POD_E.signature_ref).
 */
@Composable
fun SignaturePad(
    modifier: Modifier = Modifier,
    clearSignal: Int = 0,
    strokeColor: Color = PaperColors.paperInk,
    onPathChange: (Path?) -> Unit = {},
) {
    var path by remember(clearSignal) { mutableStateOf(Path()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(PaperColors.paperWhite, RoundedCornerShape(12.dp))
            .border(1.dp, PaperColors.paperRule, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        path = Path().apply { moveTo(offset.x, offset.y) }
                        // S27: report from the gesture, not a LaunchedEffect — lineTo mutates
                        // the Path in place, so an effect keyed on the reference never fires
                        // again and the caller saw only the empty stroke-start path.
                        onPathChange(null)
                    },
                    onDrag = { change, _ ->
                        path.lineTo(change.position.x, change.position.y)
                        onPathChange(path.takeIf { !it.isEmpty })
                    }
                )
            }
    ) {
        // Baseline hairline 32dp from the bottom
        val baselineY = size.height - 32.dp.toPx()
        drawLine(
            color = PaperColors.paperRule,
            start = Offset(0f, baselineY),
            end = Offset(size.width, baselineY),
            strokeWidth = 1.dp.toPx()
        )
        if (!path.isEmpty) {
            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}