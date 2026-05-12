package com.alertanumero.mx.telephony

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alertanumero.mx.ui.theme.AlertaNumeroMXTheme

class CallerIdOverlayActivity : ComponentActivity() {
    private val closeHandler = Handler(Looper.getMainLooper())
    private val closeRunnable = Runnable { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        closeHandler.postDelayed(closeRunnable, AUTO_CLOSE_MS)

        val rawNumber = intent.getStringExtra(EXTRA_RAW_NUMBER).orEmpty()
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val source = intent.getStringExtra(EXTRA_SOURCE).orEmpty()

        setContent {
            AlertaNumeroMXTheme {
                CallerIdOverlayContent(
                    rawNumber = rawNumber,
                    categoryLabel = categoryLabel(category),
                    label = label,
                    source = source,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onDestroy() {
        closeHandler.removeCallbacks(closeRunnable)
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_RAW_NUMBER = "extra_raw_number"
        private const val EXTRA_LABEL = "extra_label"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_SOURCE = "extra_source"
        private const val AUTO_CLOSE_MS = 8_000L

        fun start(context: Context, rawNumber: String, label: String, category: String, source: String) {
            val intent = Intent(context, CallerIdOverlayActivity::class.java).apply {
                putExtra(EXTRA_RAW_NUMBER, rawNumber)
                putExtra(EXTRA_LABEL, label)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_SOURCE, source)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            runCatching { context.startActivity(intent) }
        }

        private fun categoryLabel(category: String): String = when (category) {
            "SUSPICIOUS" -> "LLAMADA SOSPECHOSA"
            "TELEMARKETING" -> "PUBLICIDAD"
            "COLLECTION" -> "COBRANZA"
            "DIAGNOSTIC" -> "DIAGNÓSTICO"
            else -> category.ifBlank { "Sin categoría" }
        }
    }
}

@Composable
private fun CallerIdOverlayContent(
    rawNumber: String,
    categoryLabel: String,
    label: String,
    source: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ScamCall MX", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    categoryLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Número: ${rawNumber.ifBlank { "No disponible" }}", style = MaterialTheme.typography.bodyLarge)
                Text("Detalle: ${label.ifBlank { "Sin detalle" }}", style = MaterialTheme.typography.bodyMedium)
                Text("Fuente: ${source.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cerrar") }
            }
        }
    }
}
