package com.chs.yourdocscanner.result

sealed interface ScanResultIntent {
    data object ClickDelete : ScanResultIntent
    data object ClickFab : ScanResultIntent
}