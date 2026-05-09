package com.alertanumero.mx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val testNumbers = listOf("+52 55 1234 5678", "5512345678")

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
                InfoCard(title = "Identificación", subtitle = "Activación requerida") {
                    Text(text = uiState.title, style = MaterialTheme.typography.bodyMedium)
                }
            }

            item {
                InfoCard(title = "Base de datos", subtitle = "Actualizable") {
                    Text(
                        text = uiState.recordCount.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "registros",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Última actualización: ${uiState.lastUpdated}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = viewModel::refreshDatabase,
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Actualizar ahora")
                    }
                    if (uiState.isLoading) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator()
                            Text("Actualizando…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (uiState.sourceUrl.isNotBlank()) {
                        Text(
                            text = "Fuente: ${uiState.sourceUrl}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                InfoCard(title = "Busca cualquier número", subtitle = "Con o sin 52") {
                    OutlinedTextField(
                        value = uiState.phoneInput,
                        onValueChange = viewModel::onPhoneChanged,
                        label = { Text("Número") },
                        placeholder = { Text("Con o sin 52") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(onClick = viewModel::search, modifier = Modifier.fillMaxWidth()) {
                        Text("Buscar")
                    }

                    val resultText = when (uiState.queryResult) {
                        QueryStatus.SEGURO -> "Seguro"
                        QueryStatus.SOSPECHOSO -> "Sospechoso"
                        QueryStatus.NO_ENCONTRADO -> "Desconocido"
                        null -> "-"
                    }
                    Text("Resultado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(resultText, style = MaterialTheme.typography.headlineSmall)
                }
            }

            item {
                InfoCard(title = "Herramienta local de prueba", subtitle = "Números de ejemplo") {
                    Text(
                        text = "Prueba rápida sin cambiar la lógica de datos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (sample in testNumbers) {
                        Button(
                            onClick = {
                                viewModel.onPhoneChanged(sample)
                                viewModel.search()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Probar $sample")
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    subtitle: String,
    content: @Composable Column.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
