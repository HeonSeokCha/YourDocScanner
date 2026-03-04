package com.chs.yourdocscanner.scan

import android.content.Context
import android.view.Surface
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
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
import java.util.concurrent.Executors

@KoinViewModel
class DocumentScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(DocumentState())
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    private val analysisExecutor = Executors.newSingleThreadExecutor()

    fun changeIntent(intent: DocumentScanIntent) {
        when (intent) {
            is DocumentScanIntent.BindCamera -> {
                bindToCamera(intent.context, intent.lifecycle)
            }

            DocumentScanIntent.ClickCaptureButton -> TODO()
        }
    }

    private val cameraPreviewUseCase = Preview.Builder().build().apply {

        setSurfaceProvider { newSurfaceRequest ->
            _state.update { it.copy(surfaceRequest = newSurfaceRequest) }
        }
    }

    private fun bindToCamera(
        appContext: Context,
        lifecycleOwner: LifecycleOwner
    ) {
        viewModelScope.launch {
            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        analysisExecutor,
                        DocumentAnalyzer { quad, imgWidth, imgHeight ->
                            _state.update {
                                it.copy(
                                    currentDetectedQuad = quad,
                                    analysisSize = imgWidth to imgHeight,
                                )
                            }
                        }
                    )
                }

            val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)

            processCameraProvider.bindToLifecycle(
                lifecycleOwner,
                DEFAULT_BACK_CAMERA,
                cameraPreviewUseCase,
                imageAnalysis
            )

            try {
                awaitCancellation()
            } finally {
                processCameraProvider.unbindAll()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        analysisExecutor.shutdown()
    }
}


