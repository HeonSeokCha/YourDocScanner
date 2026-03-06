package com.chs.yourdocscanner

import android.graphics.Bitmap

object OpenCVBridge {
    init {
        System.loadLibrary("native-lib")
    }

    external fun detectRectangles(
        yuvData: ByteArray,
        width: Int,
        height: Int
    ): FloatArray

    external fun resetHistory()

    external fun warpDocument(bitmap: Bitmap, points: FloatArray): Bitmap?
}