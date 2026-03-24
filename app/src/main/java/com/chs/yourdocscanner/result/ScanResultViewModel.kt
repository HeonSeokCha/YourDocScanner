package com.chs.yourdocscanner.result

import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class ScanResultViewModel(
    private val cropFilePath: String,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanResultState())
    val state = _state
        .onStart {
            init(cropFilePath)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            _state.value
        )

    private val _effect: Channel<ScanResultEffect> = Channel(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()


    private fun init(path: String) {
        _state.update { it.copy(cropBitmap = BitmapFactory.decodeFile(path)) }
    }

    fun handleIntent(intent: ScanResultIntent) {
        when (intent) {
            ScanResultIntent.ClickDelete -> {
                _effect.trySend(ScanResultEffect.NavigateScan)
            }
            ScanResultIntent.ClickNext -> TODO()
        }
    }
}