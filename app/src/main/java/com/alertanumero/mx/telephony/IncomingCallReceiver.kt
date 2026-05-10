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

        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: return
        val store = CallAlertStore(context)
        val entry = store.findLocalTestEntry(incomingNumber) ?: store.findEntry(incomingNumber)
        if (entry == null) return

        val helper = AlertNotificationHelper(context)
        helper.ensureChannel()
        helper.showCallAlert(incomingNumber, entry)
    }
}
