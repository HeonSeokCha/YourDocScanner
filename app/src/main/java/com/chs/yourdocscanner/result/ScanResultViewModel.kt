package com.chs.yourdocscanner.result

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ScanResultViewModel(
    private val filePath: String
) : ViewModel() {
    private val _state = MutableStateFlow(ScanResultState())
    val state = _state
        .onStart {
            init(filePath)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            _state.value
        )


    private fun init(path: String) {
        _state.update { it.copy(currentBitmap = BitmapFactory.decodeFile(path)) }
    }

    fun handleIntent(intent: ScanResultIntent) {
        when (intent) {
            ScanResultIntent.ClickDelete -> TODO()
            ScanResultIntent.ClickNext -> TODO()
        }
    }
}