package com.chs.yourdocscanner.scan

import androidx.camera.core.SurfaceRequest

data class DocumentState(
    val surfaceRequest: SurfaceRequest? = null,
    val currentDetectedQuad: DetectedQuad? = null,
    val detectProgress: Float = 0.0f,
    val showDetectingMessage: Boolean = false
)
