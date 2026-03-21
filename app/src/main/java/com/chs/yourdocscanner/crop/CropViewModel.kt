package com.chs.yourdocscanner.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chs.yourdocscanner.OpenCVBridge
import com.chs.yourdocscanner.ScanRepository
import com.chs.yourdocscanner.scan.DetectedQuad
import com.chs.yourdocscanner.toDetectQuad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

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
            CropIntent.ClickCancel -> {}
            CropIntent.ClickConfirm -> {}
            CropIntent.ClickReset -> reduce { cropUtil.reset(it) }
            is CropIntent.DragStart -> reduce { cropUtil.dragStart(it, intent.offset, touchRadiusPx) }
            is CropIntent.DragMove -> reduce { cropUtil.dragMove(it, intent.offset) }
            CropIntent.DragEnd -> reduce { cropUtil.dragEnd(it) }
        }
    }

//    fun cropBitmap() {
//        OpenCVBridge.warpDocument()
//    }

    private fun reduce(reducer: (CropState) -> CropState) {
        _state.update(reducer)
    }
}