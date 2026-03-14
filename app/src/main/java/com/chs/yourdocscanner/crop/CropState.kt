package com.chs.yourdocscanner.crop

import android.graphics.Bitmap

data class CropState(
    val originBitmap: Bitmap? = null,
    val currentQuad: DisplayQuad = DisplayQuad()
)
