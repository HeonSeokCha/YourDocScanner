package com.chs.yourdocscanner.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.chs.yourdocscanner.scan.DetectedQuad
import kotlin.collections.indexOfFirst
import kotlin.math.roundToInt

@Composable
fun CropOverlay(
    modifier: Modifier = Modifier,
//    quad: DisplayQuad,
//    onQuadChange: (DisplayQuad) -> Unit
) {
    val density = LocalDensity.current
    val handleRadius = with(density) { 18.dp.toPx() }
    val touchRadius = with(density) { 40.dp.toPx() }

    val quad by remember { mutableStateOf(DisplayQuad()) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var offsettlX by remember { mutableFloatStateOf(0f) }
    var offsettlY by remember { mutableFloatStateOf(0f) }
    var offsettrX by remember { mutableFloatStateOf(1000f) }
    var offsettrY by remember { mutableFloatStateOf(0f) }
    var offsetblX by remember { mutableFloatStateOf(0f) }
    var offsetblY by remember { mutableFloatStateOf(1000f) }
    var offsetbrX by remember { mutableFloatStateOf(1000f) }
    var offsetbrY by remember { mutableFloatStateOf(1000f) }


    Box(
        modifier = Modifier
            .offset { IntOffset(offsettlX.roundToInt(), offsettlY.roundToInt()) }
            .background(Color.Blue, CircleShape)
            .size(24.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsettlX += dragAmount.x
                    offsettlY += dragAmount.y
                }
            }
    )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsettrX.roundToInt(), offsettrY.roundToInt()) }
                .background(Color.Blue, CircleShape)
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsettrX += dragAmount.x
                        offsettrY += dragAmount.y
                    }
                }
        )



        Box(
            modifier = Modifier
                .offset { IntOffset(offsetblX.roundToInt(), offsetblY.roundToInt()) }
                .background(Color.Blue, CircleShape)
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetblX += dragAmount.x
                        offsetblY += dragAmount.y
                    }
                }
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetbrX.roundToInt(), offsetbrY.roundToInt()) }
                .background(Color.Blue, CircleShape)
                .size(24.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetbrX += dragAmount.x
                        offsetbrY += dragAmount.y
                    }
                }
        )


//    Canvas(modifier = dragModifier) {
//        val (tl, tr, br, bl) = listOf(quad.tl, quad.tr, quad.br, quad.bl)
//
//        val maskPath = Path().apply {
//            addRect(Rect(0f, 0f, size.width, size.height))
//            moveTo(tl.x, tl.y)
//            lineTo(tr.x, tr.y)
//            lineTo(br.x, br.y)
//            lineTo(bl.x, bl.y)
//            close()
//        }
//        drawPath(
//            path = maskPath,
//            color = Color.Black.copy(alpha = 0.55f),
//            style = Fill,
//            blendMode = BlendMode.SrcOver
//        )
//
//        val borderPath = Path().apply {
//            moveTo(tl.x, tl.y)
//            lineTo(tr.x, tr.y)
//            lineTo(br.x, br.y)
//            lineTo(bl.x, bl.y)
//            close()
//        }
//        drawPath(
//            path = borderPath,
//            color = Color.White,
//            style = Stroke(width = 2.dp.toPx())
//        )
//
//        drawGridLines(tl, tr, br, bl)
//
//        quad.toList().forEachIndexed { index, corner ->
//            val isDragging = index == draggingIndex
//            val handleColor = if (isDragging) Color(0xFFFFD600) else Color.White
//            val scale = if (isDragging) 1.3f else 1.0f
//
//            drawCircle(
//                color = Color.Black.copy(alpha = 0.3f),
//                radius = handleRadius * scale + 4f,
//                center = corner
//            )
//
//            drawCircle(
//                color = handleColor,
//                radius = handleRadius * scale,
//                center = corner
//            )
//            drawCornerMark(corner = corner, index = index, size = handleRadius * scale * 0.7f)
//        }
//    }
}

private fun DrawScope.drawCornerMark(corner: Offset, index: Int, size: Float) {
    val color = Color(0xFFFFD600)
    val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)

    val (hDir, vDir) = when (index) {
        0 -> Offset(1f, 0f) to Offset(0f, 1f)   // TL: →, ↓
        1 -> Offset(-1f, 0f) to Offset(0f, 1f)   // TR: ←, ↓
        2 -> Offset(-1f, 0f) to Offset(0f, -1f)  // BR: ←, ↑
        else -> Offset(1f, 0f) to Offset(0f, -1f)  // BL: →, ↑
    }
    drawLine(color, corner, corner + hDir * size, strokeWidth = stroke.width)
    drawLine(color, corner, corner + vDir * size, strokeWidth = stroke.width)
}

private fun DrawScope.drawGridLines(
    tl: Offset,
    tr: Offset,
    br: Offset,
    bl: Offset
) {
    val gridColor = Color.White.copy(alpha = 0.35f)
    val stroke = Stroke(width = 1.dp.toPx())

    for (t in listOf(1f / 3f, 2f / 3f)) {
        val left = lerp(tl, bl, t)
        val right = lerp(tr, br, t)
        drawLine(gridColor, left, right, strokeWidth = stroke.width)
    }
    for (t in listOf(1f / 3f, 2f / 3f)) {
        val top = lerp(tl, tr, t)
        val bottom = lerp(bl, br, t)
        drawLine(gridColor, top, bottom, strokeWidth = stroke.width)
    }

}