package com.chs.yourdocscanner

object OpenCVBridge {
    init {
        System.loadLibrary("native-lib")
    }

    external fun detectRectangles(
        yData: ByteArray,
        yRowStride: Int,
        uvData: ByteArray,
        uvRowStride: Int,
        uvPixelStride: Int,
        width: Int, height: Int
    ): FloatArray

    external fun resetHistory()
}