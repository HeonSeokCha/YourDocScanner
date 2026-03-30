package com.chs.yourdocscanner.scan

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.chs.yourdocscanner.OpenCVBridge

class DocumentAnalyzer(
    private val onResult: (DetectedQuad?) -> Unit
) : ImageAnalysis.Analyzer {

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val rawBitmap = image.toBitmap()

        val raw = OpenCVBridge.detectRectangles(rawBitmap)

        val quad = if (raw != null && raw.size == 4) {
            DetectedQuad(
                topLeft = Offset(raw[0].x.toFloat(), raw[0].y.toFloat()),
                topRight = Offset(raw[1].x.toFloat(), raw[1].y.toFloat()),
                bottomRight = Offset(raw[2].x.toFloat(), raw[2].y.toFloat()),
                bottomLeft = Offset(raw[3].x.toFloat(), raw[3].y.toFloat()),
                imageWidth = image.width,
                imageHeight = image.height,
                rotationDegrees = image.imageInfo.rotationDegrees
            )
        } else null

        rawBitmap.recycle()
        image.close()
        onResult(quad)
    }
}
