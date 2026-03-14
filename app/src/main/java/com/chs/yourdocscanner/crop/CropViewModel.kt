package com.chs.yourdocscanner.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chs.yourdocscanner.ScanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class CropViewModel(
    originFilePath: String,
    originQuad: FloatArray?,
    private val scanRepository: ScanRepository
) : ViewModel() {
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


    private fun init(path: String) {
        _state.update {
            it.copy(
                originBitmap = BitmapFactory.decodeFile(path),

            )
        }
    }

    fun handleIntent(intent: CropIntent) {
        when (intent) {
            is CropIntent.OnChangeQuad -> _state.update { it.copy(currentQuad = intent.quad) }
        }
    }
}