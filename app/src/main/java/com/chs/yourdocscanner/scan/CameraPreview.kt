package com.chs.yourdocscanner.scan

import android.util.Log
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    surfaceRequest: SurfaceRequest?,
    onBindCamera: () -> Unit
) {
    LaunchedEffect(lifecycleOwner) {
        Log.e("_DEBUG", lifecycleOwner.lifecycle.currentState.name)
        onBindCamera()
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            modifier = modifier,
            surfaceRequest = request,
        )
    }
}