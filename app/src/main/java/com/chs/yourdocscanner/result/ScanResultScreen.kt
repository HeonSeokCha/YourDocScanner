package com.chs.yourdocscanner.result

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ScanResultScreenRoot(
    viewModel: ScanResultViewModel,
    onNavigateCrop: (String, FloatArray) -> Unit,
    onNavigateScan: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ScanResultEffect.NavigateCrop -> {
                    onNavigateCrop(effect.originFilePath, effect.cropPoints)
                }
                ScanResultEffect.NavigateScan -> onNavigateScan()
            }
        }
    }

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
        if (state.cropBitmap != null) {
            Image(
                modifier = Modifier
                    .align(Alignment.Center),
                bitmap = state.cropBitmap.asImageBitmap(),
                contentDescription = null
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier
                    .weight(1f),
                onClick = { onIntent(ScanResultIntent.ClickDelete) }
            ) {
                Text(text = "스캔 삭제")
            }
            Button(
                modifier = Modifier
                    .weight(1f),
                onClick = { onIntent(ScanResultIntent.ClickCrop) }
            ) {
                Text(text = "스캔 편집")
            }
        }
    }
}
