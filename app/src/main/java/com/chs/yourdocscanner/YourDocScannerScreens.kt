package com.chs.yourdocscanner

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface YourDocScannerScreens : NavKey {
    @Serializable
    data object PermissionScreen : YourDocScannerScreens
    @Serializable
    data object DocumentScannerScreen : YourDocScannerScreens
    @Serializable
    data object CropScreen : YourDocScannerScreens
    @Serializable
    data object ScanResultScreen : YourDocScannerScreens
}