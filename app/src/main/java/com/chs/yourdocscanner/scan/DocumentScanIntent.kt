package com.chs.yourdocscanner.scan

import android.content.Context
import androidx.lifecycle.LifecycleOwner

sealed interface DocumentScanIntent {

    data class BindCamera(
        val context: Context,
        val lifecycle: LifecycleOwner
    ) : DocumentScanIntent
    data class DetectDocument(val floatArray: FloatArray) : DocumentScanIntent
    data object ClickCaptureButton : DocumentScanIntent
}