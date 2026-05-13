package com.alertanumero.mx.telephony

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alertanumero.mx.R
import com.alertanumero.mx.MainActivity
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

    fun showCallAlert(rawNumber: String, entry: ScamEntry) = showCallAlert(rawNumber, entry, "CallScreeningService")

    fun showCallAlert(rawNumber: String, entry: ScamEntry, source: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val body = formatBody(rawNumber = rawNumber, category = entry.category.name, source = source)
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

    fun showIncomingOverlay(rawNumber: String, category: String, label: String, source: String) {
        runCatching {
            if (Settings.canDrawOverlays(context)) {
                CallerIdOverlayActivity.start(context, rawNumber, label, category, source)
            } else {
                showFallbackIncomingCallDetected(source)
            }
        }
    }

    fun showDiagnosticCallSeen(rawNumber: String, source: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val body = formatBody(rawNumber = rawNumber, category = "DIAGNOSTIC", source = source)
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

    fun showFallbackIncomingCallDetected(source: String = "PHONE_STATE") {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val contentIntent = PendingIntent.getActivity(context, 40, intent, flags)
        val body = "Android detectó una llamada, pero no entregó el número."
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📞 Llamada detectada")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$body\nsource: $source"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
            .setContentIntent(contentIntent)
        NotificationManagerCompat.from(context).notify(("fallback_call_detected_" + source).hashCode(), notificationBuilder.build())
    }

    fun showHeadsUpTestNotification() { /* unchanged */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val body = "Si ves esta notificación, los permisos de alerta están activos."
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📞 Prueba de notificación")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(30_000)
        NotificationManagerCompat.from(context).notify("heads_up_test_notification".hashCode(), notificationBuilder.build())
    }

    private fun formatBody(rawNumber: String, category: String, source: String): String {
        val displayNumber = rawNumber.ifBlank { "No disponible" }
        return "Número: $displayNumber\nCategoría: $category\nFuente: $source\nConsejo: No compartas códigos ni datos bancarios."
    }

    private fun titleForCategory(category: ScamCategory): String = when {
        category.name.contains("SUSP", true) -> "⚠️ Posible estafa"
        category.name.contains("PUBLIC", true) || category.name.contains("TELE", true) -> "📣 Publicidad / telemarketing"
        category.name.contains("COBRAN", true) || category.name.contains("COLLECTION", true) -> "💰 Cobranza"
        else -> "⚠️ Número sospechoso"
    }

    fun canUseFullScreenIntent(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val manager = context.getSystemService(NotificationManager::class.java)
        return runCatching { manager.canUseFullScreenIntent() }.getOrDefault(false)
    }
}
