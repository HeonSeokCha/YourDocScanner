package com.chs.yourdocscanner.crop

import android.graphics.Bitmap
import com.chs.yourdocscanner.scan.DetectedQuad

data class CropState(
    val originBitmap: Bitmap? = null,
    val currentQuad: DetectedQuad? = null
)
