package com.chs.yourdocscanner.crop

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.chs.yourdocscanner.scan.DetectedQuad

@Composable
fun CropOverlay(
    quad: DetectedQuad?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        quad ?: return@Canvas

        val canvasW = size.width
        val canvasH = size.height
        val srcW = quad.imageWidth.toFloat()
        val srcH = quad.imageHeight.toFloat()

        val displayW: Float
        val displayH: Float
        val rotatePoint: (Offset) -> Offset

        when (quad.rotationDegrees) {
            90 -> {
                displayW = srcH
                displayH = srcW
                rotatePoint = { p -> Offset(srcH - p.y, p.x) }
            }

            180 -> {
                displayW = srcW
                displayH = srcH
                rotatePoint = { p -> Offset(srcW - p.x, srcH - p.y) }
            }

            270 -> {
                displayW = srcH
                displayH = srcW
                rotatePoint = { p -> Offset(p.y, srcW - p.x) }
            }

            else -> {
                displayW = srcW
                displayH = srcH
                rotatePoint = { p -> p }
            }
        }

        val scale = maxOf(canvasW / displayW, canvasH / displayH)
        val dx = (canvasW - displayW * scale) / 2f
        val dy = (canvasH - displayH * scale) / 2f

        fun Offset.toCanvas(): Offset {
            val rotated = rotatePoint(this)
            return Offset(
                x = rotated.x * scale + dx,
                y = rotated.y * scale + dy
            )
        }

        val tl = quad.topLeft.toCanvas()
        val tr = quad.topRight.toCanvas()
        val br = quad.bottomRight.toCanvas()
        val bl = quad.bottomLeft.toCanvas()

        val color = Color(0xFFFFD600)

        val path = Path().apply {
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }

        drawPath(path, color.copy(alpha = 0.20f))
        drawPath(path, color, style = Stroke(width = 4f))

        listOf(tl, tr, br, bl).forEach { corner ->
            drawCircle(Color.White, radius = 14f, center = corner)
            drawCircle(color, radius = 9f, center = corner)
        }
    }
}