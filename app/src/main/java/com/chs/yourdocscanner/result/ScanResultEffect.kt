package com.chs.yourdocscanner.result

sealed interface ScanResultEffect {
    data class NavigateCrop(
        val originFilePath: String,
        val floatArray: FloatArray
    ) : ScanResultEffect
    data object NavigateScan : ScanResultEffect
}