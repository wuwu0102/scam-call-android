package com.alertanumero.mx.telephony

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alertanumero.mx.R
import com.alertanumero.mx.data.repository.ScamCategory
import com.alertanumero.mx.data.repository.ScamEntry

class AlertNotificationHelper(private val context: Context) {
    companion object { const val CHANNEL_ID = "scam_call_alerts" }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Alertas de llamadas sospechosas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos cuando ScamCall MX detecta números sospechosos"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showCallAlert(rawNumber: String, entry: ScamEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val title = when (entry.category) {
            ScamCategory.SUSPICIOUS -> "⚠️ Llamada sospechosa detectada"
            ScamCategory.TELEMARKETING -> "📢 Publicidad / Telemarketing"
            ScamCategory.COLLECTION -> "💳 Cobranza detectada"
        }
        var bigText = when (entry.category) {
            ScamCategory.SUSPICIOUS -> "ScamCall MX detectó un número reportado como llamada sospechosa: $rawNumber. Evita compartir datos personales."
            ScamCategory.TELEMARKETING -> "ScamCall MX detectó un número reportado como publicidad o telemarketing: $rawNumber."
            ScamCategory.COLLECTION -> "ScamCall MX detectó un número reportado como cobranza: $rawNumber. Verifica la identidad antes de compartir información."
        }
        if (entry.label.isNotBlank() && entry.label != entry.category.displayLabel) bigText += " Detalle: ${entry.label}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Número: $rawNumber. Tipo: ${entry.category.displayLabel}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(rawNumber.hashCode(), notification)
    }


    fun showDiagnosticCallSeen(rawNumber: String, source: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val displayNumber = rawNumber.ifBlank { "No disponible" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Diagnóstico ScamCall MX")
            .setContentText("Llamada detectada por $source. Número: $displayNumber")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Llamada detectada por $source. Número: $displayNumber"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(("diag_" + source + rawNumber).hashCode(), notification)
    }

    fun showSuspiciousCallAlert(rawNumber: String) {
        showCallAlert(rawNumber, ScamEntry(rawNumber, ScamCategory.SUSPICIOUS, ScamCategory.SUSPICIOUS.displayLabel))
    }
}
