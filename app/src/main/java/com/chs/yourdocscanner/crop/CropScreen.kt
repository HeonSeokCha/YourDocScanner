package com.chs.yourdocscanner.crop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CropScreenRoot(
    viewModel: CropViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CropScreen(state)
}

@Composable
fun CropScreen(
    state: CropState
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.originBitmap == null) return@Column
        Image(
            state.originBitmap.asImageBitmap(),
            contentDescription = null
        )
    }
}