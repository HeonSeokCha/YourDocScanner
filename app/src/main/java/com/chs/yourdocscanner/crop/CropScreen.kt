package com.chs.yourdocscanner.crop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chs.yourdocscanner.applyRotation
import com.chs.yourdocscanner.clampTo
import com.chs.yourdocscanner.computeFitRect
import com.chs.yourdocscanner.defaultCorners
import com.chs.yourdocscanner.indexOfMinDistanceTo
import com.chs.yourdocscanner.toCanvasCorners
import com.chs.yourdocscanner.ui.theme.YourDocScannerTheme

@Composable
fun CropScreenRoot(
    viewModel: CropViewModel,
    onNavigateResult: (String, FloatArray) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    LaunchedEffect(density) {
        viewModel.touchRadiusPx = with(density) { 36.dp.toPx() }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CropEffect.NavigateBack -> onBack()
                CropEffect.SaveError -> {}
                is CropEffect.SaveSuccess -> onNavigateResult(effect.filePath, effect.quad)
            }
        }
    }

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .background(Color.Black)
                .onSizeChanged { onIntent(CropIntent.UpdateCanvasSize(it)) }
                .pointerInput(state.imageRect) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            onIntent(CropIntent.DragStart(offset))
                        },
                        onDrag = { change, _ ->
                            onIntent(CropIntent.DragMove(change.position))
                        },
                        onDragEnd = { onIntent(CropIntent.DragEnd) },
                        onDragCancel = { onIntent(CropIntent.DragEnd) }
                    )
                }
        ) {
            if (state.bitmap == null) return@Box

            Image(
                state.bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
            )

            if (state.isReady) {
                CropOverlay(
                    corners = state.corners,
                    draggingIndex = state.draggingIdx,
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            if (state.isSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        CropBottomBar(onIntent = onIntent)
    }
}


@Composable
private fun CropBottomBar(
    onIntent: (CropIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        "자동 자르기" to Icons.Default.FitScreen,
        "자르기 없음" to Icons.Default.CropFree,
        "회전" to Icons.Default.Rotate90DegreesCw
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 16.dp
            ),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth(),
            space = 4.dp
        ) {
            options.forEachIndexed { idx, info ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = idx,
                        count = options.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        activeContentColor = Color.White,
                        inactiveContainerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        inactiveContentColor = Color.White,
                    ),
                    onClick = {
                        when (idx) {
                            0 -> onIntent(CropIntent.ClickAutoCrop)
                            1 -> onIntent(CropIntent.ClickReset)
                            2 -> onIntent(CropIntent.ClickRotate)
                        }
                    },
                    selected = false,
                    label = { Text(info.first) },
                    icon = {
                        Icon(info.second, contentDescription = null)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { onIntent(CropIntent.ClickConfirm) }
        ) {
            Text(text = "적용")
        }

    }
//    Row(
//        modifier = modifier
//            .fillMaxWidth()
//            .background(Color(0xFF1A1A1A))
//            .navigationBarsPadding()
//            .padding(16.dp),
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        verticalAlignment = Alignment.CenterVertically
//    ) {
//        Button(
//            modifier = Modifier
//                .weight(0.5f),
//            onClick = onCancel
//        ) {
//            Text("취소", color = Color.White, fontSize = 16.sp)
//        }
//
//        Button(
//            modifier = Modifier
//                .weight(0.5f),
//            onClick = onConfirm,
//            colors = ButtonDefaults.buttonColors(
//                containerColor = Color(0xFF2196F3)
//            )
//        ) {
//            Text("적용", color = Color.White, fontSize = 16.sp)
//        }
//    }
}