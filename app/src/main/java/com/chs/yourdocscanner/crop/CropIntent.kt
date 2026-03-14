package com.chs.yourdocscanner.crop

sealed interface CropIntent {
    data class OnChangeQuad(val quad: DisplayQuad) : CropIntent
}