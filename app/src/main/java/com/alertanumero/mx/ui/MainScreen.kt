package com.alertanumero.mx.ui

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showActivationGuide by remember { mutableStateOf(false) }
    var showLocalSavedDialog by remember { mutableStateOf(false) }

    fun launchIntent(intent: Intent?) {
        if (intent == null) return
        runCatching { context.startActivity(intent) }
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
                InfoCard(title = "Identificación", subtitle = "Estado del dispositivo") {
                    Text(
                        text = uiState.activation.statusText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.activation.isActive) Color(0xFF2E7D32) else Color(0xFFEF6C00),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = uiState.activation.detailText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.refreshActivationStatus()
                                showActivationGuide = true
                            },
                            shape = RoundedCornerShape(999.dp)
                        ) { Text("Cómo activar") }
                    }
                }
            }

            item {
                InfoCard(title = "Base de datos", subtitle = "Actualizable") {
                    Text(text = uiState.recordCount.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(text = "registros", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Última actualización: ${uiState.lastUpdated}", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(onClick = viewModel::refreshDatabase, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Actualizar ahora") }
                    if (uiState.isLoading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(); Text("Actualizando…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (uiState.sourceUrl.isNotBlank()) {
                        Text(text = "Fuente: ${uiState.sourceUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                InfoCard(title = "Busca cualquier número", subtitle = "Con o sin 52") {
                    OutlinedTextField(value = uiState.phoneInput, onValueChange = viewModel::onPhoneChanged, label = { Text("Número") }, placeholder = { Text("Con o sin 52") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Button(onClick = viewModel::search, modifier = Modifier.fillMaxWidth()) { Text("Buscar") }

                    val resultText = when (uiState.queryResult) {
                        QueryStatus.SEGURO -> "Seguro"
                        QueryStatus.SOSPECHOSO -> "Sospechoso"
                        QueryStatus.NO_ENCONTRADO -> "Desconocido"
                        null -> "-"
                    }
                    Text("Resultado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(resultText, style = MaterialTheme.typography.headlineSmall)
                    if (uiState.querySource.isNotBlank()) {
                        Text("Fuente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(uiState.querySource, style = MaterialTheme.typography.bodyLarge)
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
            title = { Text("Cómo activar") },
            text = {
                Text(
                    "1. Abre Configuración.\n" +
                        "2. Entra a Apps.\n" +
                        "3. Busca Alerta Número MX / ScamCall MX.\n" +
                        "4. Revisa permisos de Teléfono, ID de llamada o app predeterminada de llamadas (si aparece).\n" +
                        "5. Activa permisos o configura la app según lo permita tu dispositivo.\n\n" +
                        "La ubicación exacta puede variar según la marca y versión de Android."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    launchIntent(viewModel.roleSettingsIntent())
                    showActivationGuide = false
                }) { Text("Intentar activar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    launchIntent(viewModel.appDetailsIntent())
                    showActivationGuide = false
                }) { Text("Abrir app") }
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
