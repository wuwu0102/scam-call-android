package com.alertanumero.mx.ui

import android.app.Application
import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alertanumero.mx.data.repository.FetchResult
import com.alertanumero.mx.data.repository.ScamCategory
import com.alertanumero.mx.data.repository.ScamEntry
import com.alertanumero.mx.data.repository.ScamRepository
import com.alertanumero.mx.telephony.AlertNotificationHelper
import com.alertanumero.mx.telephony.CallAlertDiagnosticsStore
import com.alertanumero.mx.telephony.CallAlertStore
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
    val detailText: String = "Verifica permisos de llamada e identificación en tu dispositivo.",
    val callScreeningActive: Boolean = false
)

data class MainUiState(
    val title: String = "Alerta Número MX",
    val recordCount: Int = 0,
    val lastUpdated: String = "N/A",
    val phoneInput: String = "",
    val queryResult: QueryStatus? = null,
    val queryCategory: ScamCategory? = null,
    val queryLabel: String = "",
    val querySource: String = "",
    val querySourceDetail: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String = "Listo para actualizar base de datos.",
    val sourceUrl: String = "",
    val localTestInput: String = "",
    val localTestMessage: String = "",
    val compatibilityReport: String = "",
    val activation: ActivationUiState = ActivationUiState(),
    val lastCallEventText: String = ""
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository: ScamRepository = ScamRepository()
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var cachedEntries: Map<String, ScamEntry> = emptyMap()
    private val prefs = application.getSharedPreferences("local_test_tool", 0)
    private val callAlertStore = CallAlertStore(application)
    private val localNumberKey = "local_test_number"
    private val localExpiryKey = "local_test_expiry"

    init {
        cleanupExpiredLocalTestNumber()
        AlertNotificationHelper(application).ensureChannel()
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
        callAlertStore.saveLocalTestNumber(normalized)
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
        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        val callScreeningActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        } else {
            false
        }
        val active = phoneStateGranted && notificationGranted

        val detailText = when {
            callScreeningActive -> "Protección activa con identificación de llamadas."
            notificationGranted -> "Activa parcialmente. Para mejorar la detección, activa ScamCall MX como app de identificación y filtro de llamadas."
            else -> "Permite notificaciones y activa identificación de llamadas."
        }

        val activationState = ActivationUiState(
            supported = true,
            isActive = active,
            statusText = if (active) "Activa" else "Activación requerida",
            detailText = detailText,
            callScreeningActive = callScreeningActive
        )
        _uiState.update { it.copy(activation = activationState) }
    }

    fun callScreeningRoleIntent(): Intent? {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            } else {
                null
            }
        } else {
            null
        }
    }

    fun refreshLastCallAlertEvent() {
        val event = CallAlertDiagnosticsStore(getApplication()).getLastEvent()
        val text = if (event == null) {
            "Sin eventos de llamada registrados todavía."
        } else {
            "Último evento: ${event.source}\n" +
                "Número: ${event.rawNumber.ifBlank { "No disponible" }}\n" +
                "Resultado: ${if (event.matched) "Coincidió" else "No coincidió"}\n" +
                "Motivo: ${event.reason}"
        }
        _uiState.update { it.copy(lastCallEventText = text) }
    }

    fun appDetailsIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", getApplication<Application>().packageName, null)
    }

    fun activationSettingsIntents(): List<Intent> {
        val context = getApplication<Application>()
        val packageName = context.packageName

        fun safeIntent(action: String, block: (Intent.() -> Unit)? = null): Intent? = runCatching {
            Intent(action).apply { block?.invoke(this) }
        }.getOrNull()

        val candidates = listOfNotNull(
            safeIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                data = Uri.fromParts("package", packageName, null)
            },
            safeIntent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            safeIntent("android.settings.CALL_SETTINGS"),
            safeIntent("android.settings.CALL_SCREENING_SETTINGS"),
            safeIntent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
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
                        cachedEntries = result.snapshot.entries
                        callAlertStore.saveEntries(cachedEntries)
                        _uiState.update {
                            it.copy(
                                recordCount = result.snapshot.totalCount,
                                lastUpdated = result.snapshot.updatedAt,
                                statusMessage = "Base de datos actualizada correctamente. Categorías: sospechosas, publicidad y cobranza.",
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
        val localEntry = if (normalized != null) callAlertStore.findLocalTestEntry(normalized) else null
        val remoteEntry = if (normalized != null) cachedEntries[normalized] else null

        _uiState.update {
            when {
                normalized == null -> it.copy(queryResult = QueryStatus.NO_ENCONTRADO, queryCategory = null, queryLabel = "", querySource = "", querySourceDetail = "")
                localEntry != null -> it.copy(queryResult = QueryStatus.SOSPECHOSO, queryCategory = localEntry.category, queryLabel = localEntry.label, querySource = "prueba local", querySourceDetail = localEntry.source)
                remoteEntry != null -> it.copy(queryResult = QueryStatus.SOSPECHOSO, queryCategory = remoteEntry.category, queryLabel = remoteEntry.label, querySource = "base de datos", querySourceDetail = remoteEntry.source)
                cachedEntries.isEmpty() -> it.copy(queryResult = QueryStatus.NO_ENCONTRADO, queryCategory = null, queryLabel = "", querySource = "", querySourceDetail = "")
                else -> it.copy(queryResult = QueryStatus.SEGURO, queryCategory = null, queryLabel = "", querySource = "", querySourceDetail = "")
            }
        }
    }

    fun generateCompatibilityReport() {
        val context = getApplication<Application>()
        val phoneStateGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        val callScreeningActive = _uiState.value.activation.callScreeningActive

        val report = """
        ScamCall MX - Android compatibility report
        Brand: ${Build.BRAND}
        Manufacturer: ${Build.MANUFACTURER}
        Model: ${Build.MODEL}
        Device: ${Build.DEVICE}
        Android SDK: ${Build.VERSION.SDK_INT}
        Android release: ${Build.VERSION.RELEASE}
        READ_PHONE_STATE granted: $phoneStateGranted
        POST_NOTIFICATIONS granted: $notificationGranted
        Database records: ${_uiState.value.recordCount}
        Last update: ${_uiState.value.lastUpdated}
        Alert mode: notification-based MVP
        READ_CALL_LOG: not requested
        Call screening active: $callScreeningActive
        Last call event: ${_uiState.value.lastCallEventText.ifBlank { "(empty)" }}
        CallScreeningService: primary path + PHONE_STATE_CHANGED fallback
    """.trimIndent()

        _uiState.update { it.copy(compatibilityReport = report) }
    }

}