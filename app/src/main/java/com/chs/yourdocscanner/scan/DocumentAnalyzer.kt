package com.chs.yourdocscanner.scan

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Offset
import com.chs.yourdocscanner.OpenCVBridge
import java.nio.ByteBuffer

class DocumentAnalyzer(
    private val onResult: (DetectedQuad?) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val yuv = image.toNV21ByteArray()
        val raw = OpenCVBridge.detectRectangles(
            yuv,
            image.width,
            image.height
        )


        val quad = if (raw.size >= 9) {
            DetectedQuad(
                topLeft = Offset(raw[0], raw[1]),
                topRight = Offset(raw[2], raw[3]),
                bottomRight = Offset(raw[4], raw[5]),
                bottomLeft = Offset(raw[6], raw[7]),
                confidence = raw[8]
            )
        } else null

        Log.e("_DEBUG", quad.toString())
        onResult(quad)
        image.close()
    }

    private fun ImageProxy.toNV21ByteArray(): ByteArray {
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer: ByteBuffer = yPlane.buffer
        val uBuffer: ByteBuffer = uPlane.buffer
        val vBuffer: ByteBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }
}
