package com.chs.yourdocscanner.crop

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

data class CropState(
    val bitmap: Bitmap? = null,
    val canvasSize: IntSize = IntSize.Zero,
    val imageRect: Rect = Rect.Zero,
    val corners: List<Offset> = emptyList(),
    val draggingIdx: Int = -1,
    val isSaving: Boolean = false
) {
    val isReady: Boolean
        get() = bitmap != null && corners.size == 4 && imageRect != Rect.Zero
}