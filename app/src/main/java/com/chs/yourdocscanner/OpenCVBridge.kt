package com.chs.yourdocscanner

import android.graphics.Bitmap
import android.graphics.Point

object OpenCVBridge {
    init {
        System.loadLibrary("native-lib")
    }

    external fun detectRectangles(
        yuvData: ByteArray,
        width: Int,
        height: Int
    ): List<Point>?

    external fun warpDocument(bitmap: Bitmap, points: FloatArray): Bitmap?
}