package com.alertanumero.mx.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.alertanumero.mx.data.repository.ScamRepository

class IncomingCallReceiver : BroadcastReceiver() {
    companion object {
        private const val FALLBACK_DEBOUNCE_MS = 8_000L
        private const val PREFS_NAME = "incoming_call_receiver"
        private const val KEY_LAST_FALLBACK_NOTIFICATION_AT = "lastIncomingAlertAt"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val normalizedNumber = ScamRepository().normalizePhone(incomingNumber.orEmpty()).orEmpty()
        val diagnosticsStore = CallAlertDiagnosticsStore(context)
        val callAlertStore = CallAlertStore(context)
        val helper = AlertNotificationHelper(context).also { it.ensureChannel() }
        val diagnosticNotifyAllCallsEnabled = callAlertStore.isDiagnosticNotifyAllCallsEnabled()

        if (incomingNumber.isNullOrBlank()) {
            diagnosticsStore.addEvent(
                source = "PHONE_STATE",
                rawNumber = "",
                normalizedNumber = normalizedNumber,
                matched = false,
                reason = "missing_EXTRA_INCOMING_NUMBER"
            )
            maybeShowFallbackNotification(context, helper)
            if (diagnosticNotifyAllCallsEnabled) {
                helper.showDiagnosticCallSeen(
                    "PHONE_STATE activo, pero Android no entregó el número",
                    "PHONE_STATE"
                )
            }
            return
        }

        val entry = callAlertStore.findLocalTestEntry(incomingNumber) ?: callAlertStore.findEntry(incomingNumber)

        diagnosticsStore.addEvent(
            source = "PHONE_STATE",
            rawNumber = incomingNumber,
            normalizedNumber = normalizedNumber,
            matched = entry != null,
            category = entry?.category?.name.orEmpty(),
            reason = if (entry != null) "matched" else "not_found"
        )

        if (entry != null) {
            helper.showCallAlert(incomingNumber, entry, "PHONE_STATE")
        } else if (diagnosticNotifyAllCallsEnabled) {
            maybeShowFallbackNotification(context, helper)
            diagnosticsStore.addEvent(
                source = "PHONE_STATE",
                rawNumber = incomingNumber,
                normalizedNumber = normalizedNumber,
                matched = false,
                reason = "diagnostic_mode_phone_state_log_only"
            )
            helper.showDiagnosticCallSeen(incomingNumber, "PHONE_STATE")
        }
    }

    private fun maybeShowFallbackNotification(context: Context, helper: AlertNotificationHelper) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(KEY_LAST_FALLBACK_NOTIFICATION_AT, 0L)
        if (now - last < FALLBACK_DEBOUNCE_MS) return
        prefs.edit().putLong(KEY_LAST_FALLBACK_NOTIFICATION_AT, now).apply()
        helper.showFallbackIncomingCallDetected("PHONE_STATE")
    }
}
