package com.chs.yourdocscanner.scan

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DocumentScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(DocumentState())
    val state: StateFlow<DocumentState> = _state.asStateFlow()

    fun onRectDetected(points: FloatArray?) {
        _state.update { it.copy(onDetectRectPos = points) }
    }
}
