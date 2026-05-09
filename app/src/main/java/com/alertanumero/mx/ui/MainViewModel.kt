package com.alertanumero.mx.ui

import android.app.Application
import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

data class ActivationUiState(
    val supported: Boolean = false,
    val isActive: Boolean = false,
    val statusText: String = "Activación requerida",
    val detailText: String = "Verifica permisos de llamada e identificación en tu dispositivo."
)

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
    val localTestMessage: String = "",
    val activation: ActivationUiState = ActivationUiState()
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: ScamRepository = ScamRepository()
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var cachedNumbers: Set<String> = emptySet()
    private val prefs = application.getSharedPreferences("local_test_tool", 0)
    private val localNumberKey = "local_test_number"
    private val localExpiryKey = "local_test_expiry"

    init {
        cleanupExpiredLocalTestNumber()
        refreshActivationStatus()
        refreshDatabase()
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phoneInput = value) }
    }

    fun onLocalTestNumberChanged(value: String) {
        _uiState.update { it.copy(localTestInput = value) }
    }

    fun saveLocalTestNumber(): Boolean {
        val normalized = repository.normalizePhone(_uiState.value.localTestInput)
        if (normalized == null) {
            _uiState.update { it.copy(localTestMessage = "Ingresa un número válido de 10 dígitos en México.") }
            return false
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
        return true
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

    fun refreshActivationStatus() {
        val context = getApplication<Application>()
        val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else {
            null
        }

        val isRoleSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true
        val isRoleHeld = isRoleSupported && roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

        val activationState = when {
            isRoleHeld -> ActivationUiState(
                supported = true,
                isActive = true,
                statusText = "Protección activa",
                detailText = "El rol de identificación/filtro de llamadas está activo en este dispositivo."
            )
            isRoleSupported -> ActivationUiState(
                supported = true,
                isActive = false,
                statusText = "Activación requerida",
                detailText = "Este Android permite rol de filtro, pero aún no está activado para la app."
            )
            else -> ActivationUiState(
                supported = false,
                isActive = false,
                statusText = "Activación requerida",
                detailText = "La detección automática puede variar según Android, fabricante y app de Teléfono."
            )
        }
        _uiState.update { it.copy(activation = activationState) }
    }

    fun roleSettingsIntent(): Intent? {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) return null
        return roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
    }

    fun appDetailsIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", getApplication<Application>().packageName, null)
    }

    fun activationSettingsIntents(): List<Intent> {
        val context = getApplication<Application>()
        val packageName = context.packageName
        val roleIntent = roleSettingsIntent()

        fun safeIntent(action: String, block: (Intent.() -> Unit)? = null): Intent? = runCatching {
            Intent(action).apply { block?.invoke(this) }
        }.getOrNull()

        val candidates = listOfNotNull(
            roleIntent,
            safeIntent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            safeIntent("android.settings.CALL_SCREENING_SETTINGS"),
            safeIntent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
            safeIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                data = Uri.fromParts("package", packageName, null)
            },
            safeIntent(Settings.ACTION_APP_NOTIFICATION_SETTINGS) {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            },
            safeIntent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS),
            safeIntent(Settings.ACTION_SETTINGS)
        )

        return candidates.filter { intent ->
            runCatching { intent.resolveActivity(context.packageManager) != null }.getOrDefault(false)
        }
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
