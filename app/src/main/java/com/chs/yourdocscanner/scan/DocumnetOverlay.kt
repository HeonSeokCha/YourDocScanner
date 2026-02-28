package com.chs.yourdocscanner.scan

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun DocumentOverlay(
    points: FloatArray?,
    imageWidth: Int,
    imageHeight: Int,
    previewWidth: Int,
    previewHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        points ?: return@Canvas
        if (points.size < 8) return@Canvas

        val scaleX = previewWidth.toFloat() / imageWidth.toFloat()
        val scaleY = previewHeight.toFloat() / imageHeight.toFloat()

        fun toOffset(xi: Int) = Offset(
            x = points[xi * 2] * scaleX,
            y = points[xi * 2 + 1] * scaleY
        )

        val p0 = toOffset(0)
        val p1 = toOffset(1)
        val p2 = toOffset(2)
        val p3 = toOffset(3)

        val path = Path().apply {
            moveTo(p0.x, p0.y)
            lineTo(p1.x, p1.y)
            lineTo(p3.x, p3.y)
            lineTo(p2.x, p2.y)
            close()
        }

        drawPath(
            path = path,
            color = Color(0x3300C853)
        )

        drawPath(
            path = path,
            color = Color(0xFF00C853),
            style = Stroke(width = 4f)
        )

        listOf(p0, p1, p2, p3).forEach { offset ->
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = offset
            )
        }
    }
}