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
    analysisWidth: Int,
    analysisHeight: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        quad ?: return@Canvas
        if (analysisWidth == 0 || analysisHeight == 0) return@Canvas

        val viewW = size.width
        val viewH = size.height
        val imgW  = analysisWidth.toFloat()
        val imgH  = analysisHeight.toFloat()
        val scale   = maxOf(viewW / imgW, viewH / imgH)

        val offsetX = (viewW - imgW * scale) / 2f
        val offsetY = (viewH - imgH * scale) / 2f

        fun Offset.toScreen() = Offset(
            x = x * scale + offsetX,
            y = y * scale + offsetY
        )

        val tl = quad.topLeft.toScreen()
        val tr = quad.topRight.toScreen()
        val br = quad.bottomRight.toScreen()
        val bl = quad.bottomLeft.toScreen()

        val color = lerp(Color(0xFFFFD600), Color(0xFF00C853), quad.confidence)

        val path = Path().apply {
            moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
            lineTo(br.x, br.y); lineTo(bl.x, bl.y)
            close()
        }

        drawPath(path, color.copy(alpha = 0.25f))
        drawPath(path, color, style = Stroke(width = 4f))
        listOf(tl, tr, br, bl).forEach {
            drawCircle(Color.White, radius = 14f, center = it)
            drawCircle(color,       radius =  9f, center = it)
        }
    }
}