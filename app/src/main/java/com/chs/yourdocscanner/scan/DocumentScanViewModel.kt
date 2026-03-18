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
import com.chs.yourdocscanner.ScanRepository
import com.chs.yourdocscanner.applyRotation
import com.chs.yourdocscanner.toRawBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@KoinViewModel
class DocumentScannerViewModel(
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DocumentState())
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    private val _effect: Channel<ScanEffect> = Channel(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val cameraExecutor = Executors.newSingleThreadExecutor()

    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    val imageAnalysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_NV21)
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

    fun changeIntent(intent: DocumentScanIntent) {
        when (intent) {
            is DocumentScanIntent.BindCamera -> {
                bindToCamera(intent.context, intent.lifecycle)
            }

            DocumentScanIntent.ClickCapture -> captureAndCrop()

            is DocumentScanIntent.ClickCaptureModeChange -> {
                _state.update { it.copy(isAutoCapture = intent.modeState) }
            }
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
            val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)

            processCameraProvider.bindToLifecycle(
                lifecycleOwner,
                DEFAULT_BACK_CAMERA,
                cameraPreviewUseCase,
                imageAnalysis,
                imageCapture
            )

            try {
                awaitCancellation()
            } catch (e: Exception) {
                _effect.trySend(ScanEffect.OnError)
            } finally {
                processCameraProvider.unbindAll()
            }
        }
    }

    fun captureAndCrop() = viewModelScope.launch {
        val (rawBitmap, rotationDegrees) = awaitCapture()
            ?: return@launch
        val quad = _state.value.currentDetectedQuad

        if (quad != null) {
            val scaleX = rawBitmap.width.toFloat() / quad.imageWidth.toFloat()
            val scaleY = rawBitmap.height.toFloat() / quad.imageHeight.toFloat()

            val points = floatArrayOf(
                quad.topLeft.x * scaleX, quad.topLeft.y * scaleY,
                quad.topRight.x * scaleX, quad.topRight.y * scaleY,
                quad.bottomRight.x * scaleX, quad.bottomRight.y * scaleY,
                quad.bottomLeft.x * scaleX, quad.bottomLeft.y * scaleY,
            )

            val warpedBitmap = OpenCVBridge.warpDocument(rawBitmap, points)

            if (warpedBitmap == null) {
                val file = saveBitmap(rawBitmap.applyRotation(rotationDegrees))
                if (file == null) {
                    _effect.trySend(ScanEffect.OnError)
                    return@launch
                }
                _effect.trySend(ScanEffect.NavigateCrop(file.absolutePath))
                return@launch
            }
            val originFile = saveBitmap(rawBitmap.applyRotation(rotationDegrees))

            val finalBitmap = warpedBitmap.applyRotation(rotationDegrees)
            val cropFile = saveBitmap(finalBitmap)

            if (originFile == null || cropFile == null) {
                _effect.trySend(ScanEffect.OnError)
                return@launch
            }

            _effect.trySend(
                ScanEffect.NavigateScanResult(
                    originFilePath = originFile.absolutePath,
                    cropFilePath = cropFile.absolutePath,
                    detectQuad = points
                )
            )

        } else {
            val file = saveBitmap(rawBitmap)
            if (file == null) {
                _effect.trySend(ScanEffect.OnError)
                return@launch
            }

            _effect.trySend(ScanEffect.NavigateCrop(file.absolutePath))
        }
    }

    private suspend fun saveBitmap(bitmap: Bitmap): File? {
        val file: File? = scanRepository.saveImage(bitmap)
        bitmap.recycle()
        return file
    }

    private suspend fun awaitCapture(): CaptureResult? =
        suspendCancellableCoroutine { cont ->
            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val rotation = image.imageInfo.rotationDegrees
                        val bitmap = image.toRawBitmap()
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
        cameraExecutor.shutdown()
    }
}


