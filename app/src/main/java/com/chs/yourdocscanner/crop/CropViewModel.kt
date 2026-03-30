package com.chs.yourdocscanner.crop

import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chs.yourdocscanner.OpenCVBridge
import com.chs.yourdocscanner.ScanRepository
import com.chs.yourdocscanner.applyRotation
import com.chs.yourdocscanner.toDetectQuad
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel
import java.io.File

@KoinViewModel
class CropViewModel(
    private val originFilePath: String,
    private val originQuad: FloatArray?,
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val cropUtil: CropUtil = CropUtil()
    private val _state = MutableStateFlow(CropState())
    val state = _state
        .onStart {
            init(originFilePath)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            _state.value
        )

    private val _effect: Channel<CropEffect> = Channel(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    var touchRadiusPx: Float = 72f

    private fun init(path: String) {
        reduce {
            cropUtil.initialize(
                current = it,
                bitmap = BitmapFactory.decodeFile(path),
                initialQuad = originQuad?.toDetectQuad()
            )
        }
    }

    fun handleIntent(intent: CropIntent) {
        when (intent) {
            is CropIntent.UpdateCanvasSize -> {
                reduce { cropUtil.updateCanvasSize(it, intent.size) }
            }

            CropIntent.ClickCancel -> _effect.trySend(CropEffect.NavigateBack)
            CropIntent.ClickConfirm -> cropBitmap()
            CropIntent.ClickReset -> reduce { cropUtil.reset(it) }
            is CropIntent.DragStart -> reduce {
                cropUtil.dragStart(
                    it,
                    intent.offset,
                    touchRadiusPx
                )
            }

            is CropIntent.DragMove -> reduce { cropUtil.dragMove(it, intent.offset) }
            CropIntent.DragEnd -> reduce { cropUtil.dragEnd(it) }
            CropIntent.ClickAutoCrop -> autoDetectOverlay()
            CropIntent.ClickRotate -> rotateBitmap()
        }
    }

    private fun cropBitmap() {
        viewModelScope.launch {
            if (_state.value.bitmap == null) return@launch
            reduce { it.copy(isSaving = true) }
            val cornerFloatArray = cropUtil.extractImagePoints(_state.value) ?: return@launch

            val cropBitmap = withContext(Dispatchers.Default) {
                OpenCVBridge.warpDocument(
                    _state.value.bitmap!!,
                    cornerFloatArray,
                    false
                )
            }

            if (cropBitmap == null) {
                _state.value.bitmap!!.recycle()
                return@launch
            }

            val file: File? = scanRepository.saveImage(cropBitmap)
            cropBitmap.recycle()
            if (file == null) return@launch

            reduce { it.copy(isSaving = false) }
            _effect.trySend(CropEffect.SaveSuccess(file.absolutePath, cornerFloatArray))
        }
    }


    private fun rotateBitmap() {
        reduce {
            Log.e("CHS_123", ((it.rotateDegree + 90) % 360).toString())
            it.copy(
                bitmap = it.bitmap?.applyRotation(90),
                rotateDegree = (it.rotateDegree + 90) % 360
            )
        }
    }

    private fun autoDetectOverlay() {
        if (_state.value.bitmap == null) return
        viewModelScope.launch {
            val points = withContext(Dispatchers.Default) {
                OpenCVBridge.detectRectanglesFromBitmap(bitmap = _state.value.bitmap!!)
            } ?: return@launch

            reduce { it.copy(corners = points) }
        }
    }

    private fun reduce(reducer: (CropState) -> CropState) {
        _state.update(reducer)
    }
}