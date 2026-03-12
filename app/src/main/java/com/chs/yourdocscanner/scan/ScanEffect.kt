package com.chs.yourdocscanner.scan

sealed interface ScanEffect {
    data object OnError : ScanEffect
    data class NavigateCrop(val filePath: String) : ScanEffect
}