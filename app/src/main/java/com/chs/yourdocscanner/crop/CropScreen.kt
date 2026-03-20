package com.chs.yourdocscanner.crop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chs.yourdocscanner.clampTo
import com.chs.yourdocscanner.computeFitRect
import com.chs.yourdocscanner.defaultCorners
import com.chs.yourdocscanner.indexOfMinDistanceTo
import com.chs.yourdocscanner.toCanvasCorners

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
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val imageRect by remember(canvasSize, state.originBitmap) {
        derivedStateOf {
            computeFitRect(
                canvasW = canvasSize.width.toFloat(),
                canvasH = canvasSize.height.toFloat(),
                imageW = (state.originBitmap?.width ?: 0).toFloat(),
                imageH = (state.originBitmap?.height ?: 0).toFloat()
            )
        }
    }

    var corners by remember(imageRect, state.currentQuad) {
        mutableStateOf(
            state.currentQuad?.toCanvasCorners(imageRect, state.originBitmap)
                ?: imageRect.defaultCorners()
        )
    }

    var draggingIndex by remember { mutableIntStateOf(-1) }

    val touchRadiusPx = with(LocalDensity.current) { 36.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .onSizeChanged { canvasSize = it }
            .pointerInput(imageRect) {
                detectDragGestures(
                    onDragStart = { offset ->
                        draggingIndex = corners.indexOfMinDistanceTo(offset, touchRadiusPx)
                    },
                    onDrag = { change, _ ->
                        if (draggingIndex >= 0) {
                            val clamped = change.position.clampTo(imageRect)
                            corners = corners.toMutableList()
                                .also { it[draggingIndex] = clamped }
                        }
                    },
                    onDragEnd = { draggingIndex = -1 },
                    onDragCancel = { draggingIndex = -1 }
                )
            }
    ) {
        if (state.originBitmap == null) return@Box

        Image(
            state.originBitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
        )

        if (canvasSize != IntSize.Zero) {
            CropOverlay(
                corners = corners,
                draggingIndex = draggingIndex,
                modifier = Modifier
                    .fillMaxSize()
            )
        }

        CropBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            onReset = {
                corners = imageRect.defaultCorners()
            },
            onCancel = { },
            onConfirm = { }
        )
    }
}


@Composable
private fun CropBottomBar(
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) {
            Text("취소", color = Color.White, fontSize = 16.sp)
        }

        IconButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "초기화",
                tint = Color.White
            )
        }

        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2196F3)
            )
        ) {
            Text("적용", color = Color.White, fontSize = 16.sp)
        }
    }
}