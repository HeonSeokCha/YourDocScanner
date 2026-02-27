package com.chs.yourdocscanner

object OpenCVBridge {
    init {
        System.loadLibrary("native-lib")
    }

    external fun detectRectangles(
        yuvData: ByteArray,
        width: Int,
        height: Int
    ): FloatArray
}