package com.alertanumero.mx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = uiState.title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text("Registros: ${uiState.recordCount}", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Última actualización: ${uiState.lastUpdated}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Button(onClick = viewModel::refreshDatabase, enabled = !uiState.isLoading) {
                Text("Actualizar manualmente")
            }

            OutlinedTextField(
                value = uiState.phoneInput,
                onValueChange = viewModel::onPhoneChanged,
                label = { Text("Número de teléfono") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Button(onClick = viewModel::search, modifier = Modifier.fillMaxWidth()) {
                Text("Buscar")
            }

            uiState.queryResult?.let {
                val text = when (it) {
                    QueryStatus.SEGURO -> "Resultado: Seguro"
                    QueryStatus.SOSPECHOSO -> "Resultado: Sospechoso"
                    QueryStatus.NO_ENCONTRADO -> "Resultado: No encontrado"
                }
                Text(text = text, style = MaterialTheme.typography.titleMedium)
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            }

            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (uiState.sourceUrl.isNotBlank()) {
                Text(
                    text = "Fuente: ${uiState.sourceUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
