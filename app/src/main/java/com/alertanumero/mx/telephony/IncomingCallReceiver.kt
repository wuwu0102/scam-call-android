package com.alertanumero.mx.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state != TelephonyManager.EXTRA_STATE_RINGING) return

        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val diagnosticsStore = CallAlertDiagnosticsStore(context)
        val callAlertStore = CallAlertStore(context)

        if (callAlertStore.isDiagnosticNotifyAllCallsEnabled()) {
            val helper = AlertNotificationHelper(context)
            helper.ensureChannel()
            helper.showDiagnosticCallSeen(incomingNumber.orEmpty(), "PHONE_STATE_CHANGED")
        }

        if (incomingNumber.isNullOrBlank()) {
            diagnosticsStore.addEvent(
                source = "PHONE_STATE_CHANGED",
                rawNumber = "",
                matched = false,
                reason = "missing_EXTRA_INCOMING_NUMBER"
            )
            return
        }

        val entry = callAlertStore.findLocalTestEntry(incomingNumber) ?: callAlertStore.findEntry(incomingNumber)

        diagnosticsStore.addEvent(
            source = "PHONE_STATE_CHANGED",
            rawNumber = incomingNumber,
            matched = entry != null,
            reason = if (entry != null) "matched" else "not_found"
        )

        if (entry == null) return

        val helper = AlertNotificationHelper(context)
        helper.ensureChannel()
        helper.showCallAlert(incomingNumber, entry)
    }
}
