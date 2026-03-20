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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
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
    corners: List<Offset>,
    draggingIndex: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (corners.size < 4) return@Canvas

        val (tl, tr, br, bl) = corners

        val fullPath = Path().apply {
            addRect(Rect(0f, 0f, size.width, size.height))
        }
        val quadPath = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
        val dimPath = Path().apply {
            op(fullPath, quadPath, PathOperation.Difference)
        }
        drawPath(dimPath, Color(0x88000000))

        drawPath(
            quadPath, Color.White, style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
            )
        )

        val guideLen = 28f
        val guideW = 4f

        data class GuideDir(val dx1: Float, val dy1: Float, val dx2: Float, val dy2: Float)

        val guides = mapOf(
            tl to GuideDir(guideLen, 0f, 0f, guideLen),
            tr to GuideDir(-guideLen, 0f, 0f, guideLen),
            br to GuideDir(-guideLen, 0f, 0f, -guideLen),
            bl to GuideDir(guideLen, 0f, 0f, -guideLen)
        )
        guides.forEach { (corner, dir) ->
            drawLine(Color.White, corner, Offset(corner.x + dir.dx1, corner.y + dir.dy1), guideW)
            drawLine(Color.White, corner, Offset(corner.x + dir.dx2, corner.y + dir.dy2), guideW)
        }

        val gridColor = Color(0x55FFFFFF)
        for (i in 1..2) {
            val t = i / 3f
            // 가로
            val leftPt = lerp(tl, bl, t)
            val rightPt = lerp(tr, br, t)
            drawLine(gridColor, leftPt, rightPt, 1f)
            // 세로
            val topPt = lerp(tl, tr, t)
            val bottomPt = lerp(bl, br, t)
            drawLine(gridColor, topPt, bottomPt, 1f)
        }

        corners.forEachIndexed { index, corner ->
            val isDragging = index == draggingIndex
            drawCircle(
                color = if (isDragging) Color.White else Color(0xCCFFFFFF),
                radius = if (isDragging) 22f else 16f,
                center = corner,
                style = Fill
            )
            drawCircle(
                color = if (isDragging) Color(0xFF2196F3) else Color(0xFF1565C0),
                radius = if (isDragging) 12f else 8f,
                center = corner,
                style = Fill
            )
        }
    }
}