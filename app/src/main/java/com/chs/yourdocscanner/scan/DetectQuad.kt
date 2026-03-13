package com.chs.yourdocscanner.scan

import androidx.compose.ui.geometry.Offset

data class DetectedQuad(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomRight: Offset,
    val bottomLeft: Offset,
    val imageWidth: Int,
    val imageHeight: Int,
    val rotationDegrees: Int
)
