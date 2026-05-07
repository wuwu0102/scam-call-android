package com.alertanumero.mx.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alertanumero.mx.data.repository.FetchResult
import com.alertanumero.mx.data.repository.ScamRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QueryStatus { SEGURO, SOSPECHOSO, NO_ENCONTRADO }

data class MainUiState(
    val title: String = "Alerta Número MX",
    val recordCount: Int = 0,
    val lastUpdated: String = "N/A",
    val phoneInput: String = "",
    val queryResult: QueryStatus? = null,
    val isLoading: Boolean = false,
    val statusMessage: String = "Listo para actualizar base de datos.",
    val sourceUrl: String = ""
)

class MainViewModel(
    private val repository: ScamRepository = ScamRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var cachedNumbers: Set<String> = emptySet()

    init {
        refreshDatabase()
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phoneInput = value) }
    }

    fun refreshDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Actualizando datos...") }
            when (val result = repository.fetchDatabase()) {
                is FetchResult.Success -> {
                    cachedNumbers = result.snapshot.numbers
                    _uiState.update {
                        it.copy(
                            recordCount = result.snapshot.totalCount,
                            lastUpdated = result.snapshot.updatedAt,
                            statusMessage = "Base de datos actualizada correctamente.",
                            isLoading = false,
                            sourceUrl = result.snapshot.sourceUrl
                        )
                    }
                }

                is FetchResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun search() {
        val normalized = repository.normalizePhone(_uiState.value.phoneInput)
        val result = when {
            normalized == null -> QueryStatus.NO_ENCONTRADO
            cachedNumbers.contains(normalized) -> QueryStatus.SOSPECHOSO
            cachedNumbers.isEmpty() -> QueryStatus.NO_ENCONTRADO
            else -> QueryStatus.SEGURO
        }
        _uiState.update { it.copy(queryResult = result) }
    }
}
