package com.chs.yourdocscanner.scan

import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DocumentScanScreenRoot(
    viewModel: DocumentScannerViewModel
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DocumentScanScreen(
        state = state,
        onIntent = viewModel::changeIntent
    )
}

@Composable
fun DocumentScanScreen(
    state: DocumentState,
    onIntent: (DocumentScanIntent) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f)

        ) {
            CameraPreview(
                modifier = Modifier
                    .fillMaxSize(),
                surfaceRequest = state.surfaceRequest,
                onBindCamera = {
                    onIntent(DocumentScanIntent.BindCamera(context, lifecycleOwner))
                }
            )

            DocumentOverlay(
                quad = state.currentDetectedQuad,
                modifier = Modifier.fillMaxSize(),
            )
        }

        CameraBottomControls(
            modifier = Modifier.weight(0.3f),
            onCaptureClick = { onIntent(DocumentScanIntent.ClickCaptureButton) },
            isAutoCapture = state.isAutoCapture,
            onModeChange = { onIntent(DocumentScanIntent.ClickCaptureModeChange) },
        )
    }
}
