package com.alertanumero.mx.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showDiagnosticReport by remember { mutableStateOf(false) }
    val callScreeningRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshActivationStatus()
    }

    fun copyText(value: String, message: String) {
        clipboardManager.setText(AnnotatedString(value))
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun shareText(value: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, value)
        }
        startActivity(context, Intent.createChooser(shareIntent, "Compartir"), null)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            item {
                InfoCard(title = "Identificación de llamadas", subtitle = "Compatibilidad según dispositivo") {
                    val callerIdEnabled = uiState.activation.callScreeningActive
                    val estado = if (callerIdEnabled) "Activa" else "Requiere activación"
                    Text("Estado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(estado, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Identifica llamadas sospechosas usando la base de datos local.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (callerIdEnabled) {
                        Text(
                            "Identificación activada",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (!uiState.activation.roleAvailable) {
                        Text(
                            "Este dispositivo no permite activar identificación de llamadas desde esta app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Button(
                            onClick = {
                                val activationIntent = viewModel.callScreeningRoleIntent()
                                if (activationIntent != null) {
                                    callScreeningRoleLauncher.launch(activationIntent)
                                } else {
                                    viewModel.refreshActivationStatus()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Activar identificación") }
                    }
                    Text(
                        "Debido a las configuraciones de Android y de la app de teléfono, la alerta durante llamadas puede variar según el dispositivo. En algunos teléfonos puede mostrarse como notificación o no aparecer en tiempo real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                InfoCard(title = "Buscar número", subtitle = "Con o sin 52") {
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
                }
            }

            item {
                InfoCard(title = "Base de datos", subtitle = "Actualizable") {
                    Text(text = uiState.recordCount.toString(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text(text = "registros", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "Última actualización: ${uiState.lastUpdated}", style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Button(onClick = viewModel::refreshDatabase, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Actualizar ahora") }
                    if (uiState.isLoading) {
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(); Text("Actualizando…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                InfoCard(title = "Comentarios y diagnóstico", subtitle = "Ayúdanos a revisar compatibilidad") {
                    Text("Si la alerta no aparece durante una llamada, puedes generar un reporte para ayudarnos a mejorar la compatibilidad.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { viewModel.generateCompatibilityReport(); showDiagnosticReport = true }, modifier = Modifier.fillMaxWidth()) { Text("Generar reporte diagnóstico") }
                    Button(onClick = { showDiagnosticReport = true }, modifier = Modifier.fillMaxWidth()) { Text("Enviar comentario") }

                    if (showDiagnosticReport && uiState.compatibilityReport.isNotBlank()) {
                        OutlinedTextField(value = uiState.compatibilityReport, onValueChange = {}, modifier = Modifier.fillMaxWidth(), readOnly = true, label = { Text("Reporte diagnóstico") })
                        Button(onClick = { copyText(uiState.compatibilityReport, "Reporte copiado") }, modifier = Modifier.fillMaxWidth()) { Text("Copiar reporte") }
                        Button(onClick = { shareText(uiState.compatibilityReport) }, modifier = Modifier.fillMaxWidth()) { Text("Compartir reporte") }
                    }

                    OutlinedTextField(
                        value = uiState.feedbackInput,
                        onValueChange = viewModel::onFeedbackChanged,
                        label = { Text("Comentario") },
                        placeholder = { Text("Describe qué pasó en tu teléfono") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        val payload = viewModel.buildCommentWithReport()
                        copyText(payload, "Comentario con reporte copiado")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Copiar comentario con reporte") }
                    Button(onClick = { shareText(viewModel.buildCommentWithReport()) }, modifier = Modifier.fillMaxWidth()) { Text("Compartir comentario con reporte") }
                }
            }

            item {
                InfoCard(title = "Nota de compatibilidad Android", subtitle = "Comportamiento según fabricante y app de teléfono") {
                    Text(
                        "La visualización de alertas depende de Android, permisos, rol de identificación y la app de teléfono predeterminada. La experiencia puede variar entre dispositivos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
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
