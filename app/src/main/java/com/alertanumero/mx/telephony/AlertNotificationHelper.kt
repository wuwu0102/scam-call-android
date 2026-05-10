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

class AlertNotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "scam_call_alerts"
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de llamadas sospechosas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos cuando ScamCall MX detecta números sospechosos"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showSuspiciousCallAlert(rawNumber: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("⚠️ Llamada sospechosa detectada")
            .setContentText("Número: $rawNumber. Evita compartir datos personales.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "ScamCall MX detectó un número reportado como sospechoso: $rawNumber. " +
                        "Si no reconoces la llamada, cuelga y bloquea el número."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(rawNumber.hashCode(), notification)
    }
}
