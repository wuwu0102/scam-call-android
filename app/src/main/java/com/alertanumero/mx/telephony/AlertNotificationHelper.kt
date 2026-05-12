package com.alertanumero.mx.telephony

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alertanumero.mx.R
import com.alertanumero.mx.data.repository.ScamCategory
import com.alertanumero.mx.data.repository.ScamEntry

class AlertNotificationHelper(private val context: Context) {
    companion object { const val CHANNEL_ID = "incoming_call_alerts" }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Alertas de llamadas sospechosas", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos cuando ScamCall MX detecta números sospechosos"
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showCallAlert(rawNumber: String, entry: ScamEntry) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val body = formatBody(
            rawNumber = rawNumber,
            category = entry.category.name,
            label = entry.label,
            source = "CallScreeningService"
        )

        val fullScreenIntent = buildOverlayPendingIntent(rawNumber, entry.label, entry.category.name, "CallScreeningService")
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Alerta de llamada")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
        if (canUseFullScreenIntent()) {
            notificationBuilder.setFullScreenIntent(fullScreenIntent, true)
        } else {
            notificationBuilder.setContentText("$body · Activa la alerta en pantalla completa")
        }
        NotificationManagerCompat.from(context).notify(rawNumber.hashCode(), notificationBuilder.build())
    }


    fun showDiagnosticCallSeen(rawNumber: String, source: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val body = formatBody(
            rawNumber = rawNumber,
            category = "DIAGNOSTIC",
            label = "Diagnóstico: llamada detectada",
            source = source
        )
        val fullScreenIntent = buildOverlayPendingIntent(rawNumber, "Diagnóstico: llamada detectada", "DIAGNOSTIC", source)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Diagnóstico de llamada")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
        if (canUseFullScreenIntent()) {
            notificationBuilder.setFullScreenIntent(fullScreenIntent, true)
        } else {
            notificationBuilder.setContentText("$body · Activa la alerta en pantalla completa")
        }
        NotificationManagerCompat.from(context).notify(("diag_" + source + rawNumber).hashCode(), notificationBuilder.build())
    }

    fun showSuspiciousCallAlert(rawNumber: String) {
        showCallAlert(rawNumber, ScamEntry(rawNumber, ScamCategory.SUSPICIOUS, ScamCategory.SUSPICIOUS.displayLabel))
    }

    private fun formatBody(rawNumber: String, category: String, label: String, source: String): String {
        val displayNumber = rawNumber.ifBlank { "No disponible" }
        val displayLabel = label.ifBlank { "Sin detalle" }
        return "Número: $displayNumber · Categoría: $category · Etiqueta: $displayLabel · source = $source"
    }

    private fun buildOverlayPendingIntent(rawNumber: String, label: String, category: String, source: String): PendingIntent {
        val intent = Intent(context, CallerIdOverlayActivity::class.java).apply {
            putExtra("extra_raw_number", rawNumber)
            putExtra("extra_label", label)
            putExtra("extra_category", category)
            putExtra("extra_source", source)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, ("overlay_" + rawNumber + category).hashCode(), intent, flags)
    }

    fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        return runCatching { manager.canUseFullScreenIntent() }.getOrDefault(false)
    }
}
