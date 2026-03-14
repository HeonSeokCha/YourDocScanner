package com.chs.yourdocscanner.crop

import androidx.compose.ui.geometry.Offset

data class DisplayQuad(
    val tl: Offset = Offset(0f, 500f),
    val tr: Offset = Offset(500f, 0f),
    val br: Offset = Offset(500f, 500f),
    val bl: Offset = Offset(500f, 0f),
) {
    fun toList() = listOf(tl, tr, br, bl)
    fun replace(index: Int, offset: Offset) = when (index) {
        0    -> copy(tl = offset)
        1    -> copy(tr = offset)
        2    -> copy(br = offset)
        else -> copy(bl = offset)
    }
}