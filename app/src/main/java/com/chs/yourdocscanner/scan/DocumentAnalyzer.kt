package com.chs.yourdocscanner.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.chs.yourdocscanner.OpenCVBridge

class DocumentAnalyzer(
    private val onResult: (quad: DetectedQuad?, imageWidth: Int, imageHeight: Int) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val yPlane  = image.planes[0]
        val uPlane  = image.planes[1]

        val yData  = yPlane.buffer.toByteArray()
        val uvData = uPlane.buffer.toByteArray()

        val raw = OpenCVBridge.detectRectangles(
            yData,  yPlane.rowStride,
            uvData, uPlane.rowStride, uPlane.pixelStride,
            image.width, image.height
        )

        val quad = if (raw.size >= 9 && raw[8] > 0f) {
            DetectedQuad(
                topLeft     = Offset(raw[0], raw[1]),
                topRight    = Offset(raw[2], raw[3]),
                bottomRight = Offset(raw[4], raw[5]),
                bottomLeft  = Offset(raw[6], raw[7]),
                confidence  = raw[8]
            )
        } else null

        onResult(quad, image.width, image.height)
        image.close()
    }

    private fun java.nio.ByteBuffer.toByteArray(): ByteArray {
        rewind(); return ByteArray(remaining()).also { get(it) }
    }
}
