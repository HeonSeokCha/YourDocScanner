package com.chs.yourdocscanner.scan

import android.content.Context
import androidx.lifecycle.LifecycleOwner

sealed interface DocumentScanIntent {

    data class BindCamera(
        val context: Context,
        val lifecycle: LifecycleOwner
    ) : DocumentScanIntent

    data object ClickCapture : DocumentScanIntent
    data class ClickCaptureModeChange(val modeState: Boolean): DocumentScanIntent
}