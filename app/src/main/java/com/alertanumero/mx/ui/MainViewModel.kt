package com.alertanumero.mx.ui

import android.app.Application
import android.Manifest
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alertanumero.mx.BuildConfig
import com.alertanumero.mx.data.repository.FetchResult
import com.alertanumero.mx.data.repository.ScamCategory
import com.alertanumero.mx.data.repository.ScamEntry
import com.alertanumero.mx.data.repository.ScamRepository
import com.alertanumero.mx.telephony.AlertNotificationHelper
import com.alertanumero.mx.telephony.CallAlertDiagnosticsStore
import com.alertanumero.mx.telephony.CallAlertStore
import com.alertanumero.mx.telephony.LastCallAlertEvent
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
    val callScreeningActive: Boolean = false,
    val roleAvailable: Boolean = false,
    val roleHeld: Boolean = false,
    val serviceDeclared: Boolean = false,
    val servicePermission: String = "",
    val serviceExported: Boolean = false,
    val fullScreenAlertPermissionGranted: Boolean = true,
    val serviceComponentName: String = "",
    val packageName: String = "",
    val appLabel: String = "",
    val defaultDialerPackage: String = "unknown",
    val isGoogleDialerDefault: Boolean = false,
    val queryIntentServicesCount: Int = 0,
    val roleActiveButServiceNotInvoked: Boolean = false
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
    val localTestRawInput: String = "",
    val localTestMessage: String = "",
    val localTestNormalized: String = "",
    val localTestAliases: String = "",
    val compatibilityReport: String = "",
    val feedbackInput: String = "",
    val activation: ActivationUiState = ActivationUiState(),
    val callScreeningInvoked: Boolean = false,
    val automaticCallAlertStatus: String = "",
    val lastCallEventText: String = "",
    val lastCallScreeningEventText: String = "none yet",
    val lastFallbackEventText: String = "none yet",
    val fallbackPhoneStateDetected: Boolean = false,
    val fallbackNumberAvailable: Boolean = false,
    val fallbackOnlyMessage: String = "",
    val recentCallEventsText: String = "",
    val diagnosticNotifyAllCalls: Boolean = false,
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
        _uiState.update { it.copy(diagnosticNotifyAllCalls = callAlertStore.isDiagnosticNotifyAllCallsEnabled()) }
        refreshDatabase()
    }

    fun onPhoneChanged(value: String) {
        _uiState.update { it.copy(phoneInput = value) }
    }

    fun onLocalTestNumberChanged(value: String) {
        _uiState.update { it.copy(localTestInput = value, localTestRawInput = value) }
    }

    fun onFeedbackChanged(value: String) {
        _uiState.update { it.copy(feedbackInput = value) }
    }

    fun saveLocalTestNumber(): Boolean {
        val rawInput = _uiState.value.localTestInput
        val normalized = repository.normalizePhone(rawInput)
        if (normalized == null) {
            _uiState.update { it.copy(localTestMessage = "Ingresa un número válido.") }
            return false
        }
        val aliases = repository.lookupAliases(rawInput)

        val expiry = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        prefs.edit()
            .putString(localNumberKey, normalized)
            .putLong(localExpiryKey, expiry)
            .apply()

        _uiState.update {
            it.copy(
                localTestInput = normalized,
                localTestRawInput = rawInput,
                localTestMessage = "Prueba local guardada. Número normalizado: $normalized",
                localTestNormalized = normalized,
                localTestAliases = aliases.joinToString(", ")
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
        val roleManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getSystemService(RoleManager::class.java) else null
        val roleAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true else false
        val roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true else false
        val expectedServiceClass = "com.alertanumero.mx.telephony.ScamCallScreeningService"
        val servicePermissionRequired = "android.permission.BIND_SCREENING_SERVICE"
        val serviceIntent = Intent("android.telecom.CallScreeningService").setPackage(context.packageName)
        val queriedServices = context.packageManager.queryIntentServices(serviceIntent, PackageManager.GET_META_DATA)
        val declaredServiceInfo = queriedServices.firstOrNull { info ->
            info.serviceInfo?.name == expectedServiceClass ||
                info.serviceInfo?.name?.endsWith(".ScamCallScreeningService") == true
        }?.serviceInfo
        val serviceDeclared = declaredServiceInfo != null
        val serviceExported = declaredServiceInfo?.exported == true
        val serviceComponentName = declaredServiceInfo?.let { "${it.packageName}/${it.name}" }
            ?: "${context.packageName}/$expectedServiceClass"
        val telecomManager = context.getSystemService(TelecomManager::class.java)
        val defaultDialerPackage = telecomManager?.defaultDialerPackage ?: "unknown"
        val isGoogleDialerDefault = defaultDialerPackage == "com.google.android.dialer"
        val fullScreenAlertPermissionGranted = AlertNotificationHelper(context).canUseFullScreenIntent()
        val roleActiveButServiceNotInvoked = roleHeld &&
            CallAlertDiagnosticsStore(context)
                .getRecentEvents()
                .none { it.source == "CallScreeningService" }
        val callScreeningActive = roleAvailable && roleHeld && serviceDeclared
        val active = phoneStateGranted && notificationGranted
        val appLabel = context.packageManager.getApplicationLabel(context.applicationInfo).toString()

        val detailText = when {
            roleActiveButServiceNotInvoked -> "Google Phone has not bound the CallScreeningService yet."
            callScreeningActive -> "Protección activa con identificación de llamadas."
            notificationGranted -> "Activa parcialmente. Para mejorar la detección, activa ScamCall MX como app de identificación y filtro de llamadas."
            else -> "Permite notificaciones y activa identificación de llamadas."
        }

        val activationState = ActivationUiState(
            supported = true,
            isActive = active,
            statusText = if (active) "Activa" else "Activación requerida",
            detailText = detailText,
            callScreeningActive = callScreeningActive,
            roleAvailable = roleAvailable,
            roleHeld = roleHeld,
            serviceDeclared = serviceDeclared,
            servicePermission = declaredServiceInfo?.permission ?: servicePermissionRequired,
            serviceExported = serviceExported,
            fullScreenAlertPermissionGranted = fullScreenAlertPermissionGranted,
            serviceComponentName = serviceComponentName,
            packageName = context.packageName,
            appLabel = appLabel,
            defaultDialerPackage = defaultDialerPackage,
            isGoogleDialerDefault = isGoogleDialerDefault,
            queryIntentServicesCount = queriedServices.size,
            roleActiveButServiceNotInvoked = roleActiveButServiceNotInvoked
        )
        _uiState.update { it.copy(activation = activationState) }
        refreshRecentCallAlertEvents()
    }

    fun callScreeningRoleIntent(): Intent? {
        val context = getApplication<Application>()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                null
            } else if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                null
            } else {
                roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            }
        } else {
            null
        }
    }

    fun refreshRecentCallAlertEvents() {
        val events = CallAlertDiagnosticsStore(getApplication()).getRecentEvents()
        val recent = events.take(10)
        val hasCallScreeningEvent = recent.any { it.source == "CallScreeningService" }
        val hasPhoneStateMissingNumber = recent.any {
            (it.source == "PHONE_STATE_CHANGED" || it.source == "PHONE_STATE") && it.reason.contains("missing_EXTRA_INCOMING_NUMBER")
        }
        val lastCallScreeningEvent = events.firstOrNull { it.source == "CallScreeningService" }
        val lastFallbackEvent = events.firstOrNull {
            it.source == "PHONE_STATE_CHANGED" || it.source == "PHONE_STATE" || it.source == "IncomingCallReceiver"
        }
        val fallbackPhoneStateDetected = lastFallbackEvent != null
        val fallbackNumberAvailable = lastFallbackEvent?.rawNumber?.isNotBlank() == true
        val automaticStatus = when {
            hasCallScreeningEvent -> "CallScreeningService recibió llamadas en este dispositivo."
            _uiState.value.activation.roleHeld && hasPhoneStateMissingNumber ->
                "El rol está activo, pero aún no se ha registrado una llamada real mediante CallScreeningService."
            _uiState.value.activation.roleHeld ->
                "Google Phone has not bound the CallScreeningService yet."
            else -> "Manual lookup only on this device."
        }
        val text = if (events.isEmpty()) {
            "Sin eventos de llamada registrados todavía."
        } else {
            events.joinToString("\n\n") { event ->
                "Evento: ${event.source}\n" +
                    "Número (raw): ${event.rawNumber.ifBlank { "No disponible" }}\n" +
                    "Número normalizado: ${event.normalizedNumber.ifBlank { "No disponible" }}\n" +
                    "Resultado DB: ${if (event.matched) "Coincidió" else "No coincidió"}\n" +
                "Categoría: ${event.category.ifBlank { "N/A" }}\n" +
                "Motivo: ${event.reason}\n" +
                "Dirección: ${event.callDirection.ifBlank { "N/A" }} · incoming: ${event.isIncoming}\n" +
                "Scheme: ${event.handleScheme.ifBlank { "N/A" }} · Presentation: ${event.handlePresentation.ifBlank { "N/A" }}\n" +
                "Timestamp: ${event.timestamp}"
            }
        }
        val fallbackOnlyMessage = if (!hasCallScreeningEvent && fallbackPhoneStateDetected) {
            "Modo compatible activo: llamada detectada sin número."
        } else ""
        _uiState.update {
            it.copy(
                recentCallEventsText = text,
                lastCallEventText = text,
                lastCallScreeningEventText = lastCallScreeningEvent?.let { formatEventLine(it) } ?: "none yet",
                lastFallbackEventText = lastFallbackEvent?.let { formatEventLine(it) } ?: "none yet",
                fallbackPhoneStateDetected = fallbackPhoneStateDetected,
                fallbackNumberAvailable = fallbackNumberAvailable,
                fallbackOnlyMessage = fallbackOnlyMessage,
                callScreeningInvoked = hasCallScreeningEvent,
                automaticCallAlertStatus = automaticStatus
            )
        }
    }

    private fun formatEventLine(event: LastCallAlertEvent): String {
        return "${event.source} · ${event.reason} · ${event.rawNumber.ifBlank { "No disponible" }} · ${event.timestamp}"
    }


    fun setDiagnosticNotifyAllCalls(enabled: Boolean) {
        callAlertStore.setDiagnosticNotifyAllCalls(enabled)
        _uiState.update { it.copy(diagnosticNotifyAllCalls = enabled) }
    }

    fun clearDiagnosticEvents() {
        CallAlertDiagnosticsStore(getApplication()).clearEvents()
        refreshRecentCallAlertEvents()
    }


    fun showTestNotification() {
        val helper = AlertNotificationHelper(getApplication())
        helper.ensureChannel()
        helper.showHeadsUpTestNotification()
    }

    fun openReactivationSettingsIntent(): Intent {
        val context = getApplication<Application>()
        val callScreeningIntent = Intent("android.settings.CALL_SCREENING_SETTINGS")
        if (callScreeningIntent.resolveActivity(context.packageManager) != null) return callScreeningIntent

        val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        if (defaultAppsIntent.resolveActivity(context.packageManager) != null) return defaultAppsIntent

        return appDetailsIntent()
    }

    fun refreshCallScreeningComponent(): Boolean {
        val context = getApplication<Application>()
        val component = ComponentName(context, "com.alertanumero.mx.telephony.ScamCallScreeningService")
        val success = runCatching {
            context.packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            context.packageManager.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }.isSuccess
        refreshActivationStatus()
        return success
    }

    fun defaultAppsIntent(): Intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)

    fun phoneAppSettingsIntent(): Intent = Intent("android.settings.CALL_SETTINGS")

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

    fun fullScreenIntentSettingsIntent(): Intent? {
        val context = getApplication<Application>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        val packageName = context.packageName
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:$packageName")
        }
        return if (intent.resolveActivity(context.packageManager) != null) intent else null
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
        val aliases = repository.lookupAliases(_uiState.value.phoneInput)
        val localEntry = if (normalized != null) callAlertStore.findLocalTestEntry(normalized) else null
        val remoteEntry = aliases.firstNotNullOfOrNull { alias -> cachedEntries[alias] }

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
        _uiState.update { it.copy(compatibilityReport = buildDiagnosticReport()) }
    }

    fun buildCommentWithReport(): String {
        val comment = _uiState.value.feedbackInput.trim()
        val commentSection = if (comment.isBlank()) {
            "Comentario del usuario:\n(Sin comentario)"
        } else {
            "Comentario del usuario:\n$comment"
        }
        return "$commentSection\n\n${buildDiagnosticReport()}"
    }

    private fun buildDiagnosticReport(): String {
        val context = getApplication<Application>()
        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        val activation = _uiState.value.activation
        val lastCallScreeningEvent = _uiState.value.lastCallScreeningEventText
        val lastPhoneStateEvent = _uiState.value.lastFallbackEventText

        return """
        ScamCall MX - Reporte diagnóstico
        App name: ${activation.appLabel}
        App versionName: ${BuildConfig.VERSION_NAME}
        App versionCode: ${BuildConfig.VERSION_CODE}
        package name: ${activation.packageName}
        device manufacturer: ${Build.MANUFACTURER}
        device model: ${Build.MODEL}
        Android version: ${Build.VERSION.RELEASE}
        SDK version: ${Build.VERSION.SDK_INT}
        notification permission status: ${if (notificationGranted) "granted" else "not granted"}
        CallScreening role available: ${if (activation.roleAvailable) "yes" else "no"}
        CallScreening role held by this app: ${if (activation.roleHeld) "yes" else "no"}
        service declared: ${if (activation.serviceDeclared) "yes" else "no"}
        service exported: ${if (activation.serviceExported) "yes" else "no"}
        default dialer package: ${activation.defaultDialerPackage}
        database record count: ${_uiState.value.recordCount}
        database last updated time: ${_uiState.value.lastUpdated}
        last CallScreeningService event: $lastCallScreeningEvent
        last PHONE_STATE fallback event: $lastPhoneStateEvent
        recent call diagnostic events:
        ${_uiState.value.recentCallEventsText}
        timestamp: ${System.currentTimeMillis()}
        """.trimIndent()
    }

}
