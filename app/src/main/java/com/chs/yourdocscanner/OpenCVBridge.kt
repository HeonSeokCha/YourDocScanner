package com.chs.yourdocscanner

import android.graphics.Bitmap
import android.graphics.Point
import androidx.compose.ui.geometry.Offset

object OpenCVBridge {
    init {
        System.loadLibrary("native-lib")
    }

    external fun detectRectangles(bitmap: Bitmap): List<Point>?

    external suspend fun detectRectanglesFromBitmap(bitmap: Bitmap): List<Offset>?

    external suspend fun warpDocument(
        bitmap: Bitmap,
        points: FloatArray,
        isFlip: Boolean
    ): Bitmap?
}