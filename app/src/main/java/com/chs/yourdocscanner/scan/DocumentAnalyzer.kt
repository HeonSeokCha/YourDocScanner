package com.chs.yourdocscanner.scan

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.chs.yourdocscanner.OpenCVBridge
import java.nio.ByteBuffer

class DocumentAnalyzer(
    private val onRectDetected: (FloatArray?) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        val yuvBytes = image.toNV21ByteArray()
        val result = OpenCVBridge.detectRectangles(
            yuvBytes,
            image.width,
            image.height
        )
        onRectDetected(if (result.isEmpty()) null else result)
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

        // NV21 = Y + VU interleaved
        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }
}
