package com.chs.yourdocscanner.scan

import android.content.Context
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DocumentScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(DocumentState())
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    fun changeIntent(intent: DocumentScanIntent) {
        when (intent) {
            is DocumentScanIntent.BindCamera -> {
                bindToCamera(intent.context, intent.lifecycle)
            }
            is DocumentScanIntent.DetectDocument -> onRectDetected(intent.floatArray)
            DocumentScanIntent.ClickCaptureButton -> TODO()
        }
    }

    private fun onRectDetected(points: FloatArray?) {
        _state.update { it.copy(onDetectRectPos = points) }
    }

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _state.update { it.copy(surfaceRequest = newSurfaceRequest) }
        }
    }

    private fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)
            processCameraProvider.bindToLifecycle(
                lifecycleOwner,
                DEFAULT_BACK_CAMERA,
                cameraPreviewUseCase
            )

            try {
                awaitCancellation()
            } finally {
                processCameraProvider.unbindAll()
            }
        }
    }
}


