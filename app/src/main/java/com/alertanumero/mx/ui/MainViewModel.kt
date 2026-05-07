package com.alertanumero.mx.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MainUiState(
    val title: String = "Alerta Número MX",
    val subtitle: String = "Fase 1 基礎架構已就緒",
    val statusMessage: String = "尚未啟用資料下載。"
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
}
