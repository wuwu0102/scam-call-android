package com.alertanumero.mx.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.alertanumero.mx.data.repository.ScamRepository

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val normalizedNumber = ScamRepository().normalizePhone(incomingNumber.orEmpty()).orEmpty()
        val diagnosticsStore = CallAlertDiagnosticsStore(context)
        val callAlertStore = CallAlertStore(context)

        if (incomingNumber.isNullOrBlank()) {
            diagnosticsStore.addEvent(
                source = "PHONE_STATE",
                rawNumber = "",
                normalizedNumber = normalizedNumber,
                matched = false,
                reason = "missing_EXTRA_INCOMING_NUMBER"
            )
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

        if (entry == null && callAlertStore.isDiagnosticNotifyAllCallsEnabled()) {
            diagnosticsStore.addEvent(
                source = "PHONE_STATE",
                rawNumber = incomingNumber,
                normalizedNumber = normalizedNumber,
                matched = false,
                reason = "diagnostic_mode_phone_state_log_only"
            )
        }
    }
}
