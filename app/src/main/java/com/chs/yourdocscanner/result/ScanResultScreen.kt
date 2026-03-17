package com.chs.yourdocscanner.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScanResultScreenRoot(
    viewModel: ScanResultViewModel,
    onNavigateCrop: () -> Unit,
    onNavigateScan: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScanResultScreen(
        state = state,
        onIntent = viewModel::handleIntent
    )
}

@Composable
fun ScanResultScreen(
    state: ScanResultState,
    onIntent: (ScanResultIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.currentBitmap != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center),
                bitmap = state.currentBitmap.asImageBitmap(),
                contentDescription = null
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Button() { }
            Button() { }
        }
    }
}
