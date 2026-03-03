package com.chs.yourdocscanner.scan

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp

@Composable
fun DocumentOverlay(
    quad: DetectedQuad?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        quad ?: return@Canvas

//        val sx = previewWidth.toFloat() / imageWidth
//        val sy = previewHeight.toFloat() / imageHeight

        fun Offset.scale() = Offset(x, y)

        val tl = quad.topLeft.scale()
        val tr = quad.topRight.scale()
        val br = quad.bottomRight.scale()
        val bl = quad.bottomLeft.scale()

        val overlayColor = lerp(Color(0xFFFFD600), Color(0xFF00C853), quad.confidence)

        val path = Path().apply {
            moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
            lineTo(br.x, br.y); lineTo(bl.x, bl.y)
            close()
        }
        drawPath(path, overlayColor.copy(alpha = 0.25f))
        drawPath(path, overlayColor, style = Stroke(width = 4f))

        listOf(tl, tr, br, bl).forEach {
            drawCircle(Color.White, radius = 12f, center = it)
            drawCircle(overlayColor, radius = 8f, center = it)
        }
    }
}