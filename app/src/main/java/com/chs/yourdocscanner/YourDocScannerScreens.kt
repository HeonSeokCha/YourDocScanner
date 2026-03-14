package com.chs.yourdocscanner

import androidx.navigation3.runtime.NavKey
import com.chs.yourdocscanner.scan.DetectedQuad
import kotlinx.serialization.Serializable

@Serializable
sealed interface YourDocScannerScreens : NavKey {
    @Serializable
    data object PermissionScreen : YourDocScannerScreens
    @Serializable
    data object DocumentScannerScreen : YourDocScannerScreens
    @Serializable
    data class CropScreen(
        val filePath: String,
        val floatArray: FloatArray? = null
    ) : YourDocScannerScreens
    @Serializable
    data object ScanResultScreen : YourDocScannerScreens
}