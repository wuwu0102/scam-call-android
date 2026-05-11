package com.alertanumero.mx.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

class CallScreeningBootstrapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        runCatching {
            val serviceIntent = Intent(context, CallScreeningBootstrapService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            val telecomManager = context.getSystemService(TelecomManager::class.java)
            CallAlertDiagnosticsStore(context).addEvent(
                source = "BOOT_COMPLETED",
                rawNumber = "",
                normalizedNumber = "",
                matched = false,
                reason = if (telecomManager != null) "bootstrap_started" else "telecom_manager_unavailable"
            )
        }
    }
}
