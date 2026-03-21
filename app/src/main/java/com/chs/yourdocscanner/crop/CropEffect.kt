package com.chs.yourdocscanner.crop

sealed interface CropEffect {
    data class SaveSuccess(val filePath: String, val quad: FloatArray) : CropEffect
    object SaveError : CropEffect
    object NavigateBack : CropEffect
}