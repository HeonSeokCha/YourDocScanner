package com.chs.yourdocscanner.scan

import android.graphics.Bitmap

data class CaptureResult(
    val bitmap: Bitmap,
    val rotationDegrees: Int
)
