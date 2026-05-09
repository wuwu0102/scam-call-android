package com.alertanumero.mx.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val querySource: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String = "Listo para actualizar base de datos.",
    val sourceUrl: String = "",
    val localTestInput: String = "",
    val localTestMessage: String = ""
)

class MainViewModel(
    application: Application,
    private val repository: ScamRepository = ScamRepository()
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var cachedNumbers: Set<String> = emptySet()
    private val prefs = application.getSharedPreferences("local_test_tool", 0)
    private val localNumberKey = "local_test_number"
    private val localExpiryKey = "local_test_expiry"

    init {
        cleanupExpiredLocalTestNumber()
        refreshDatabase()
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phoneInput = value) }
    }

    fun onLocalTestNumberChanged(value: String) {
        _uiState.update { it.copy(localTestInput = value) }
    }

    fun saveLocalTestNumber() {
        val normalized = repository.normalizePhone(_uiState.value.localTestInput)
        if (normalized == null) {
            _uiState.update { it.copy(localTestMessage = "Ingresa un número válido de 10 dígitos en México.") }
            return
        }

        val expiry = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        prefs.edit()
            .putString(localNumberKey, normalized)
            .putLong(localExpiryKey, expiry)
            .apply()

        _uiState.update {
            it.copy(
                localTestInput = normalized,
                localTestMessage = "Número de prueba guardado localmente por 24 horas."
            )
        }
    }

    private fun cleanupExpiredLocalTestNumber() {
        val expiry = prefs.getLong(localExpiryKey, 0L)
        if (expiry > 0L && System.currentTimeMillis() > expiry) {
            prefs.edit().remove(localNumberKey).remove(localExpiryKey).apply()
            _uiState.update { it.copy(localTestMessage = "La prueba local expiró y fue eliminada.") }
        }
    }

    private fun activeLocalTestNumber(): String? {
        cleanupExpiredLocalTestNumber()
        val expiry = prefs.getLong(localExpiryKey, 0L)
        if (expiry <= 0L || System.currentTimeMillis() > expiry) return null
        return prefs.getString(localNumberKey, null)
    }

    fun refreshDatabase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = "Actualizando datos...") }
            try {
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = "No se pudo actualizar la base de datos. Error: ${e.message ?: "desconocido"}"
                    )
                }
            }
        }
    }

    fun search() {
        val normalized = repository.normalizePhone(_uiState.value.phoneInput)
        val localTest = activeLocalTestNumber()
        val result = when {
            normalized == null -> QueryStatus.NO_ENCONTRADO
            localTest != null && localTest == normalized -> QueryStatus.SOSPECHOSO
            cachedNumbers.contains(normalized) -> QueryStatus.SOSPECHOSO
            cachedNumbers.isEmpty() -> QueryStatus.NO_ENCONTRADO
            else -> QueryStatus.SEGURO
        }
        val source = when {
            normalized == null -> ""
            localTest != null && localTest == normalized -> "prueba local"
            cachedNumbers.contains(normalized) -> "base de datos"
            else -> ""
        }
        _uiState.update { it.copy(queryResult = result, querySource = source) }
    }
}
