package com.chs.yourdocscanner.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun ScanResultScreenRoot() {

}

@Composable
fun ScanResultScreen(
    state: ScanResultState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (state.currentBitmap != null) {
            Image(
                bitmap = state.currentBitmap.asImageBitmap(),
                contentDescription = null
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) { }
    }
}
