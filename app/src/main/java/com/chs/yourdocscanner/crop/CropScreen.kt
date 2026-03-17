package com.chs.yourdocscanner.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CropScreenRoot(
    viewModel: CropViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CropScreen(
        state = state,
        onIntent = viewModel::handleIntent
    )
}

@Composable
fun CropScreen(
    state: CropState,
    onIntent: (CropIntent) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {

        if (state.originBitmap == null) return@Box
        Image(
            state.originBitmap.asImageBitmap(),
            contentDescription = null
        )

        CropOverlay(
            modifier = Modifier.fillMaxSize(),
//            quad = state.currentQuad
        )
    }
}