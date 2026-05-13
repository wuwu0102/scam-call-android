package com.alertanumero.mx.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showActivationGuide by remember { mutableStateOf(false) }
    var showLocalSavedDialog by remember { mutableStateOf(false) }
    var showAdvancedDiagnostics by remember { mutableStateOf(false) }

    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshActivationStatus()
    }
    val callScreeningLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshActivationStatus()
    }

    fun safeLaunch(intent: android.content.Intent?) {
        if (intent == null || intent.resolveActivity(context.packageManager) == null) {
            Toast.makeText(context, "No se pudo abrir esta configuración.", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { settingsLauncher.launch(intent) }
            .onFailure { Toast.makeText(context, "No se pudo abrir esta configuración.", Toast.LENGTH_SHORT).show() }
    }

    fun openActivationSettings() {
        val intents = viewModel.activationSettingsIntents()
        intents.firstOrNull()?.let {
            safeLaunch(it)
        } ?: run {
            Toast.makeText(context, "No se encontró una pantalla de configuración compatible.", Toast.LENGTH_SHORT).show()
            viewModel.refreshActivationStatus()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshActivationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold { innerPadding: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "ScamCall MX",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item {
                Text(
                    text = "Evita llamadas sospechosas",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item {
                Text(
                    text = "Consulta e identifica números reportados en México desde tu teléfono.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            item {
                InfoCard(title = "Activación de llamadas", subtitle = "Sigue estos 4 pasos") {
                    val notificationReady = uiState.activation.isActive
                    val testStatus = when {
                        uiState.callScreeningInvoked -> "Detectado"
                        uiState.fallbackPhoneStateDetected -> "Solo PHONE_STATE"
                        else -> "Esperando llamada real"
                    }
                    Text("Permiso de notificaciones: ${if (notificationReady) "Listo" else "Falta"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Caller ID: ${if (uiState.activation.roleHeld) "Listo" else "Falta"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Estado de prueba: $testStatus", style = MaterialTheme.typography.bodyMedium)
                    Text("Paso 1 — Permiso de notificaciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (notificationReady) {
                        Text("Notificaciones activadas", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                    } else {
                        Button(onClick = { showActivationGuide = true }, modifier = Modifier.fillMaxWidth()) { Text("Activar notificaciones") }
                    }
                    Text("Paso 2 — Identificación de llamadas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (uiState.activation.roleHeld) {
                        Text("Listo", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                    } else {
                        Button(
                            onClick = {
                                val intent = viewModel.callScreeningRoleIntent()
                                if (intent != null) runCatching { callScreeningLauncher.launch(intent) }.onFailure { openActivationSettings() } else openActivationSettings()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Activar identificación de llamadas") }
                    }
                    Text("Paso 3 — Reiniciar teléfono", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (uiState.activation.roleHeld && !uiState.callScreeningInvoked) {
                        Text(
                            "Reinicia el teléfono una vez después de activar Caller ID. En algunos Android/Google Phone, el servicio no se enlaza hasta reiniciar。",
                            color = Color(0xFFEF6C00),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("Pendiente de primera llamada real", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF6C00))
                        Button(onClick = { safeLaunch(viewModel.openReactivationSettingsIntent()) }, modifier = Modifier.fillMaxWidth()) { Text("Reactivar identificación") }
                        Text("Si ya está activado pero no detecta llamadas, desactiva y vuelve a seleccionar Alerta Número MX como app de identificación de llamadas, luego reinicia el teléfono。", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Listo", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))
                    }
                    Text("Paso 4 — Llamada de prueba", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Haz una llamada real desde otro teléfono físico. No uses WhatsApp, LINE, VoIP ni Wi-Fi Calling。", style = MaterialTheme.typography.bodySmall)
                    Text("Después de reiniciar el teléfono, la primera llamada puede tardar unos segundos más en mostrar la alerta. Haz una segunda llamada de prueba para confirmar.", style = MaterialTheme.typography.bodySmall)
                    Text("Si solo aparece PHONE_STATE, Google Phone todavía no entregó la llamada a CallScreeningService。", style = MaterialTheme.typography.bodySmall)

                    TextButton(onClick = { showAdvancedDiagnostics = !showAdvancedDiagnostics }) {
                        Text(if (showAdvancedDiagnostics) "Ocultar diagnóstico avanzado" else "Diagnóstico avanzado")
                    }
                    if (showAdvancedDiagnostics) {
                        Text(
                            text = "role held by this app: ${if (uiState.activation.roleHeld) "yes" else "no"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "queryIntentServices(CallScreeningService): ${uiState.activation.queryIntentServicesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "service exported: ${if (uiState.activation.serviceExported) "yes" else "no"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "component: ${uiState.activation.serviceComponentName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "package: ${uiState.activation.packageName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                val ok = viewModel.refreshCallScreeningComponent()
                                Toast.makeText(context, if (ok) "Servicio actualizado. Vuelve a seleccionar la app si Android lo solicita." else "No se pudo actualizar el servicio.", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Actualizar servicio") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.refreshActivationStatus()
                                showActivationGuide = true
                            },
                            shape = RoundedCornerShape(999.dp)
                        ) { Text("Ver guía de activación") }
                    }
                    Button(
                        onClick = viewModel::refreshRecentCallAlertEvents,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Revisar eventos de llamada") }
                    Button(
                        onClick = viewModel::showTestNotification,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Probar notificación") }
                    Button(
                        onClick = viewModel::clearDiagnosticEvents,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Limpiar eventos de prueba") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = uiState.diagnosticNotifyAllCalls,
                            onCheckedChange = viewModel::setDiagnosticNotifyAllCalls
                        )
                        Text(
                            text = "Modo diagnóstico (opcional): notificar cualquier llamada",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Solo úsalo si la identificación de llamadas falla durante pruebas reales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Solo para pruebas. Muestra una notificación aunque el número no esté en la base de datos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Después de activar Caller ID, se recomienda reiniciar el teléfono una vez para que Google Phone / Android Telecom vuelva a enlazar CallScreeningService.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "En Pixel/Google Phone, la pantalla nativa de llamada puede cubrir ventanas flotantes. Por eso la alerta principal se muestra como notificación visible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "A. Real caller ID path",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "CallScreeningService = ruta oficial de identificación de llamadas (éxito real de Caller ID).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (uiState.callScreeningInvoked) "Caller ID activo. Si no ves una ventana flotante, revisa la notificación superior." else "last CallScreeningService event: ${uiState.lastCallScreeningEventText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "B. Fallback only",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "PHONE_STATE = ruta de respaldo: solo detecta que hubo llamada, no confirma identificación oficial.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "PHONE_STATE detected: ${if (uiState.fallbackPhoneStateDetected) "yes" else "no"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "number available: ${if (uiState.fallbackNumberAvailable) "yes" else "no"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.callScreeningInvoked.not() && uiState.fallbackPhoneStateDetected) {
                        Text(
                            text = "Solo PHONE_STATE: Android detectó la llamada, pero no entregó número por esta ruta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.fallbackOnlyMessage.isNotBlank()) {
                        Text(
                            text = uiState.fallbackOnlyMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "last fallback event: ${uiState.lastFallbackEventText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uiState.lastCallScreeningEventText == "none yet") {
                        Text(
                            text = "Aún no se ha recibido una llamada mediante CallScreeningService. Haz una llamada real desde otro teléfono después de activar el rol.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.lastFallbackEventText.contains("missing_EXTRA_INCOMING_NUMBER")) {
                        Text(
                            text = "Google Phone has not bound the CallScreeningService yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Checklist: ScamCall MX selected as Call Screening app · Call from another physical phone · Do not test with WhatsApp/VoIP · Test with a number not saved in contacts · Make sure the call reaches the native Phone app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.recentCallEventsText.isNotBlank()) {
                        OutlinedTextField(
                            value = uiState.recentCallEventsText,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            label = { Text("Eventos recientes") }
                        )
                    }
                }
            }

            item {
                InfoCard(title = "Base de datos", subtitle = "Actualizable") {
                    Text(text = uiState.recordCount.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(text = "registros", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Última actualización: ${uiState.lastUpdated}", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(text = "Clasificación incluida: llamadas sospechosas, publicidad/telemarketing y cobranza.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = viewModel::refreshDatabase, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Actualizar ahora") }
                    if (uiState.isLoading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(); Text("Actualizando…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                InfoCard(title = "Busca cualquier número", subtitle = "Con o sin 52") {
                    OutlinedTextField(value = uiState.phoneInput, onValueChange = viewModel::onPhoneChanged, label = { Text("Número") }, placeholder = { Text("Con o sin 52") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = viewModel::search, modifier = Modifier.fillMaxWidth()) { Text("Buscar") }

                    val resultText = when (uiState.queryResult) {
                        QueryStatus.SEGURO -> "Seguro"
                        QueryStatus.SOSPECHOSO -> uiState.queryCategory?.displayLabel ?: "Sospechoso"
                        QueryStatus.NO_ENCONTRADO -> "Desconocido"
                        null -> "-"
                    }
                    Text("Resultado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(resultText, style = MaterialTheme.typography.headlineSmall)
                    val queryCategory = uiState.queryCategory
                    if (uiState.queryResult == QueryStatus.SOSPECHOSO && queryCategory != null) {
                        Text("Tipo: ${queryCategory.shortLabel}", style = MaterialTheme.typography.bodyLarge)
                        if (uiState.queryLabel.isNotBlank()) Text("Detalle: ${uiState.queryLabel}", style = MaterialTheme.typography.bodyMedium)
                        if (uiState.querySource.isNotBlank()) Text("Fuente: ${uiState.querySource}", style = MaterialTheme.typography.bodyMedium)
                        if (uiState.querySourceDetail.isNotBlank()) Text("Origen: ${uiState.querySourceDetail}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }


            item {
                InfoCard(title = "Reporte de compatibilidad", subtitle = "Ayuda a mejorar la detección en Android") {
                    Text("Si las alertas no aparecen durante llamadas entrantes, genera este reporte y envíalo al desarrollador. No se sube automáticamente.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = viewModel::generateCompatibilityReport, modifier = Modifier.fillMaxWidth()) { Text("Generar reporte diagnóstico (1 clic)") }
                    if (uiState.compatibilityReport.isNotBlank()) {
                        OutlinedTextField(value = uiState.compatibilityReport, onValueChange = {}, modifier = Modifier.fillMaxWidth(), readOnly = true, label = { Text("Reporte") })
                    }
                }
            }

            item {
                InfoCard(title = "Herramienta local de prueba", subtitle = "Solo en este dispositivo") {
                    Text(text = "Guarda temporalmente un número en este teléfono para verificar la identificación de llamadas. No se sube ni se comparte. Se elimina automáticamente después de 24 horas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = uiState.localTestInput, onValueChange = viewModel::onLocalTestNumberChanged, label = { Text("Número de prueba") }, placeholder = { Text("Ej. 525512345678") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = {
                        if (viewModel.saveLocalTestNumber()) showLocalSavedDialog = true
                    }, modifier = Modifier.fillMaxWidth()) { Text("Guardar prueba local") }
                    Text(text = "Raw input: ${uiState.localTestRawInput.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Normalized number: ${uiState.localTestNormalized.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Aliases generated: ${uiState.localTestAliases.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (uiState.localTestMessage.isNotBlank()) {
                        Text(text = uiState.localTestMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Text(text = uiState.statusMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp), maxLines = 4, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }

    if (showActivationGuide) {
        AlertDialog(
            onDismissRequest = { showActivationGuide = false },
            title = { Text("Activación de llamadas") },
            text = {
                Text(
                    "1. Permite las notificaciones para ScamCall MX.\n" +
                        "2. Otorga permiso de estado del teléfono y, si está disponible, lectura de llamadas.\n" +
                        "3. Mantén activa la app para recibir alertas durante llamadas entrantes.\n\n" +
                        "En algunos dispositivos Android, el número entrante puede no estar disponible por restricciones del sistema.\n" +
                        "Si Android o tu marca no permiten leer el número, ScamCall MX seguirá funcionando para búsqueda manual y actualización de base de datos."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    openActivationSettings()
                    showActivationGuide = false
                }) { Text("Abrir configuración") }
            },
            dismissButton = {
                TextButton(onClick = { showActivationGuide = false }) { Text("Cerrar") }
            }
        )
    }

    if (showLocalSavedDialog) {
        AlertDialog(
            onDismissRequest = { showLocalSavedDialog = false },
            title = { Text("Prueba local guardada") },
            text = {
                Text(
                    "El número de prueba se guardó por 24 horas en este dispositivo.\n\n" +
                        "Número normalizado: ${uiState.localTestNormalized.ifBlank { "No disponible" }}\n\n" +
                        "No se sube ni se comparte.\n\n" +
                        "En Android, el identificador que se muestre durante una llamada puede variar según la app de Teléfono, la marca del dispositivo y los contactos guardados.\n\n" +
                        "Para probar mejor, usa un número que no esté guardado en tus contactos."
                )
            },
            confirmButton = {
                TextButton(onClick = { showLocalSavedDialog = false }) { Text("Entendido") }
            }
        )
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
