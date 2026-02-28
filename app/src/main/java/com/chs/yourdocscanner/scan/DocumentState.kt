package com.chs.yourdocscanner.scan

data class DocumentState(
    val onDetectRectPos: FloatArray? = null,
    val detectProgress: Float = 0.0f,
    val showDetectingMessage: Boolean = false
)
