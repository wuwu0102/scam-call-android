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
    companion object { const val CHANNEL_ID = "call_alerts" }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Alertas de llamadas sospechosas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos cuando ScamCall MX detecta números sospechosos"
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showCallAlert(rawNumber: String, entry: ScamEntry) {
        showCallAlert(rawNumber, entry, "CallScreeningService")
    }

    fun showCallAlert(rawNumber: String, entry: ScamEntry, source: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val body = formatBody(
            rawNumber = rawNumber,
            category = entry.category.name,
            source = source
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleForCategory(entry.category))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(false)
            .setTimeoutAfter(30_000)
        NotificationManagerCompat.from(context).notify(rawNumber.hashCode(), notificationBuilder.build())
    }



    fun showDiagnosticCallSeen(rawNumber: String, source: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val body = formatBody(
            rawNumber = rawNumber,
            category = "DIAGNOSTIC",
            source = source
        )
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Caller ID activo")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(false)
            .setOngoing(false)
            .setTimeoutAfter(30_000)
        NotificationManagerCompat.from(context).notify(("diag_" + source + rawNumber).hashCode(), notificationBuilder.build())
    }

    fun showSuspiciousCallAlert(rawNumber: String) {
        showCallAlert(rawNumber, ScamEntry(rawNumber, ScamCategory.SUSPICIOUS, ScamCategory.SUSPICIOUS.displayLabel))
    }

    private fun formatBody(rawNumber: String, category: String, source: String): String {
        val displayNumber = rawNumber.ifBlank { "No disponible" }
        return "Número: $displayNumber\nCategoría: $category\nsource: $source"
    }

    private fun titleForCategory(category: ScamCategory): String {
        val label = category.displayLabel
        val name = category.name.lowercase()
        return when {
            name.contains("susp") || label.contains("sospech", ignoreCase = true) -> "⚠️ Llamada sospechosa"
            name.contains("public") || name.contains("tele") ||
                label.contains("publicidad", ignoreCase = true) ||
                label.contains("telemarketing", ignoreCase = true) -> "📣 Publicidad / telemarketing"
            name.contains("cobran") || label.contains("cobranza", ignoreCase = true) -> "💰 Cobranza"
            else -> "⚠️ Número reportado"
        }
    }
}
