package com.chs.yourdocscanner.scan

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chs.yourdocscanner.OpenCVBridge
import com.chs.yourdocscanner.applyRotation
import com.chs.yourdocscanner.toRawBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@KoinViewModel
class DocumentScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(DocumentState())
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    fun changeIntent(intent: DocumentScanIntent) {
        when (intent) {
            is DocumentScanIntent.BindCamera -> {
                bindToCamera(intent.context, intent.lifecycle)
            }

            DocumentScanIntent.ClickCaptureButton -> { }
            DocumentScanIntent.ClickCapture -> { }
            DocumentScanIntent.ClickCaptureModeChange -> { _state.update { it.copy(isAutoCapture = !it.isAutoCapture) }}
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
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(
                        cameraExecutor,
                        DocumentAnalyzer { quad ->
                            _state.update {
                                it.copy(currentDetectedQuad = quad)
                            }
                        }
                    )
                }

            val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)

            processCameraProvider.bindToLifecycle(
                lifecycleOwner,
                DEFAULT_BACK_CAMERA,
                cameraPreviewUseCase,
                imageAnalysis,

            )

            try {
                awaitCancellation()
            } finally {
                processCameraProvider.unbindAll()
            }
        }
    }

    suspend fun captureAndCrop(quad: DetectedQuad) = withContext(Dispatchers.IO) {

            val (rawBitmap, rotationDegrees) = awaitCapture()
                ?: return@withContext null

            val scaleX = rawBitmap.width.toFloat()  / quad.imageWidth.toFloat()
            val scaleY = rawBitmap.height.toFloat() / quad.imageHeight.toFloat()

            val points = floatArrayOf(
                quad.topLeft.x     * scaleX,  quad.topLeft.y     * scaleY,
                quad.topRight.x    * scaleX,  quad.topRight.y    * scaleY,
                quad.bottomRight.x * scaleX,  quad.bottomRight.y * scaleY,
                quad.bottomLeft.x  * scaleX,  quad.bottomLeft.y  * scaleY,
            )

            val warpedBitmap = OpenCVBridge.warpDocument(rawBitmap, points)
                ?: return@withContext null
            rawBitmap.recycle()

            val finalBitmap = warpedBitmap.applyRotation(rotationDegrees)
        }

    private suspend fun awaitCapture(): CaptureResult? =
        suspendCancellableCoroutine { cont ->
            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap   = image.toRawBitmap()
                        image.close()
                        if (bitmap != null) {
                            cont.resume(CaptureResult(bitmap, rotation)) {}
                        } else {
                            cont.cancel(IOException("Bitmap decode failed"))
                        }
                    }
                    override fun onError(e: ImageCaptureException) {
                        cont.cancel(e)
                    }
                }
            )
        }

    override fun onCleared() {
        super.onCleared()
        OpenCVBridge.resetHistory()
        cameraExecutor.shutdown()
    }
}


