package com.chs.yourdocscanner.scan

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    surfaceRequest: SurfaceRequest?,
    onBindCamera: () -> Unit
) {
    LaunchedEffect(lifecycleOwner) {
        onBindCamera()
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            modifier = modifier,
            surfaceRequest = request
        )
    }
}