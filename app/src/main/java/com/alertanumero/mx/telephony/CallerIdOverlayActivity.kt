package com.alertanumero.mx.telephony

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alertanumero.mx.ui.theme.AlertaNumeroMXTheme

class CallerIdOverlayActivity : ComponentActivity() {
    private val closeHandler = Handler(Looper.getMainLooper())
    private val closeRunnable = Runnable { finish() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        closeHandler.postDelayed(closeRunnable, 10_000)
        setContent {
            AlertaNumeroMXTheme {
                OverlayCard(
                    rawNumber = intent.getStringExtra(EXTRA_RAW_NUMBER).orEmpty(),
                    category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty(),
                    source = intent.getStringExtra(EXTRA_SOURCE).orEmpty(),
                    onClose = { finish() }
                )
            }
        }
    }
    override fun onDestroy() { closeHandler.removeCallbacks(closeRunnable); super.onDestroy() }
    companion object {
        private const val EXTRA_RAW_NUMBER = "extra_raw_number"
        private const val EXTRA_LABEL = "extra_label"
        private const val EXTRA_CATEGORY = "extra_category"
        private const val EXTRA_SOURCE = "extra_source"
        fun start(context: Context, rawNumber: String, label: String, category: String, source: String) {
            val intent = Intent(context, CallerIdOverlayActivity::class.java).apply {
                putExtra(EXTRA_RAW_NUMBER, rawNumber); putExtra(EXTRA_LABEL, label); putExtra(EXTRA_CATEGORY, category); putExtra(EXTRA_SOURCE, source)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            runCatching { context.startActivity(intent) }
        }
    }
}

@Composable
private fun OverlayCard(rawNumber: String, category: String, source: String, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0x55000000)).statusBarsPadding().padding(16.dp), verticalArrangement = Arrangement.Top) {
        Card(modifier = Modifier.fillMaxWidth().clickable { onClose() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (rawNumber.isBlank()) "📞 Llamada detectada" else "⚠️ Posible estafa", fontWeight = FontWeight.Bold)
                Text(if (rawNumber.isBlank()) "Android detectó una llamada, pero no entregó el número." else "Número: $rawNumber")
                if (rawNumber.isNotBlank()) Text("Categoría: ${category.ifBlank { "N/A" }}")
                Text("Fuente: ${source.ifBlank { "N/A" }}")
                Text("Consejo: No compartas códigos ni datos bancarios.")
            }
        }
    }
}
