package com.chs.yourdocscanner.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.chs.yourdocscanner.OpenCVBridge
import com.chs.yourdocscanner.toNV21ByteArray

class DocumentAnalyzer(
    private val onResult: (DetectedQuad?) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val yuv = image.toNV21ByteArray()
        val raw = OpenCVBridge.detectRectangles(yuv, image.width, image.height)

        val quad = if (raw.size >= 9) {
            DetectedQuad(
                topLeft = Offset(raw[0], raw[1]),
                topRight = Offset(raw[2], raw[3]),
                bottomRight = Offset(raw[4], raw[5]),
                bottomLeft = Offset(raw[6], raw[7]),
                confidence = raw[8],
                imageWidth = image.width,
                imageHeight = image.height,
                rotationDegrees = image.imageInfo.rotationDegrees
            )
        } else null

        onResult(quad)
        image.close()
    }
}
