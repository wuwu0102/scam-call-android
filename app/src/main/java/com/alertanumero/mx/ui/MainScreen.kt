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
                InfoCard(title = "Alertas por notificación", subtitle = "Estado del dispositivo") {
                    Text(
                        text = uiState.activation.statusText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.activation.isActive) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (uiState.activation.roleHeld && !uiState.callScreeningInvoked) {
                            "Permiso activado. La búsqueda manual y la base de datos están activas. La alerta automática durante llamadas depende del sistema del teléfono."
                        } else {
                            uiState.activation.detailText
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "CallScreeningService: ${if (uiState.activation.callScreeningActive) "active" else "inactive"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "service declared: ${if (uiState.activation.serviceDeclared) "yes" else "no"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "service permission: ${uiState.activation.servicePermission}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "role held: ${if (uiState.activation.roleHeld) "yes" else "no"} · role available: ${if (uiState.activation.roleAvailable) "yes" else "no"}",
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
                        text = "package: ${uiState.activation.packageName} · app: ${uiState.activation.appLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "default dialer package: ${uiState.activation.defaultDialerPackage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!uiState.activation.isGoogleDialerDefault) {
                        Text(
                            text = "Advertencia: se recomienda usar com.google.android.dialer como app de teléfono predeterminada en Pixel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF6C00)
                        )
                    }
                    Text(
                        text = "role available: ${if (uiState.activation.roleAvailable) "yes" else "no"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    if (uiState.activation.roleHeld && !uiState.callScreeningInvoked) {
                        Text(
                            text = "El rol está activo, pero aún no se ha registrado una llamada real mediante CallScreeningService.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFEF6C00)
                        )
                    }
                    Text(
                        text = "En algunos dispositivos Pixel/Android, además del permiso de rol, debes verificar que ScamCall MX esté seleccionado como app de identificación de llamadas en Apps predeterminadas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.refreshActivationStatus()
                                showActivationGuide = true
                            },
                            shape = RoundedCornerShape(999.dp)
                        ) { Text(if (uiState.activation.roleHeld) "Revisar configuración" else "Activar alertas") }
                    }
                    if (!uiState.activation.roleHeld) {
                        Button(
                            onClick = {
                                val intent = viewModel.callScreeningRoleIntent()
                                if (intent != null) {
                                    runCatching { callScreeningLauncher.launch(intent) }
                                        .onFailure { openActivationSettings() }
                                } else {
                                    openActivationSettings()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Activar identificación de llamadas") }
                    } else {
                        Text(
                            text = "Call Screening role already granted",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { safeLaunch(viewModel.defaultAppsIntent()) }, modifier = Modifier.fillMaxWidth()) { Text("Abrir apps predeterminadas") }
                    Button(onClick = { safeLaunch(viewModel.phoneAppSettingsIntent()) }, modifier = Modifier.fillMaxWidth()) { Text("Abrir configuración de teléfono") }
                    Button(onClick = { safeLaunch(viewModel.appDetailsIntent()) }, modifier = Modifier.fillMaxWidth()) { Text("Abrir configuración de la app") }
                    Button(
                        onClick = viewModel::refreshRecentCallAlertEvents,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Revisar eventos de llamada") }
                    Button(
                        onClick = viewModel::showTestNotification,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Probar notificación") }
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
                            text = "Modo diagnóstico: notificar cualquier llamada",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Solo para pruebas. Muestra una notificación aunque el número no esté en la base de datos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "last call screening event: ${uiState.lastCallScreeningEventText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    Button(onClick = viewModel::generateCompatibilityReport, modifier = Modifier.fillMaxWidth()) { Text("Generar reporte") }
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
            title = { Text("Activar alertas") },
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
