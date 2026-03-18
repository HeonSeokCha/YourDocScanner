package com.chs.yourdocscanner.scan

sealed interface ScanEffect {
    data object OnError : ScanEffect
    data class NavigateCrop(val filePath: String) : ScanEffect
    data class NavigateScanResult(
        val originFilePath: String,
        val cropFilePath: String,
        val detectQuad: FloatArray
    ) : ScanEffect
}