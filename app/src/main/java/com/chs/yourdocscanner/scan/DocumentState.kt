package com.chs.yourdocscanner.scan

import androidx.camera.core.SurfaceRequest

data class DocumentState(
    val surfaceRequest: SurfaceRequest? = null,
    val currentDetectedQuad: DetectedQuad? = null,
    val analysisSize: Pair<Int, Int> = 0 to 0,
    val showDetectingMessage: Boolean = false
)
