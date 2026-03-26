package com.chs.yourdocscanner.crop

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize

sealed interface CropIntent {
    data class UpdateCanvasSize(val size: IntSize) : CropIntent

    data class DragStart(val offset: Offset) : CropIntent
    data class DragMove(val offset: Offset) : CropIntent
    object DragEnd : CropIntent

    object ClickReset : CropIntent
    object ClickAutoCrop : CropIntent
    object ClickRotate : CropIntent
    object ClickConfirm : CropIntent
    object ClickCancel : CropIntent
}