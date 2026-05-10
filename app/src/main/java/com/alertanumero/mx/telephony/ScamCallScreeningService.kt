package com.alertanumero.mx.telephony

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.telecom.PhoneAccount
import android.util.Log

class ScamCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        try {
            val isIncoming = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                callDetails.callDirection == Call.Details.DIRECTION_INCOMING
            } else {
                true
            }

            if (!isIncoming) {
                respondAllow(callDetails)
                return
            }

            val handle = callDetails.handle
            val rawNumber = if (handle?.scheme == PhoneAccount.SCHEME_TEL) {
                handle.schemeSpecificPart.orEmpty()
            } else {
                ""
            }

            val diagnosticsStore = CallAlertDiagnosticsStore(this)
            diagnosticsStore.addEvent(
                source = "CallScreeningService",
                rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty(),
                matched = false,
                reason = "onScreenCall_entered"
            )

            if (CallAlertStore(this).isDiagnosticNotifyAllCallsEnabled()) {
                val helper = AlertNotificationHelper(this)
                helper.ensureChannel()
                helper.showDiagnosticCallSeen(rawNumber, "CallScreeningService")
            }

            if (rawNumber.isBlank()) {
                diagnosticsStore.addEvent(
                    source = "CallScreeningService",
                    rawNumber = "",
                    matched = false,
                    reason = "empty_or_unavailable_number"
                )
                respondAllow(callDetails)
                return
            }

            val store = CallAlertStore(this)
            val entry = store.findLocalTestEntry(rawNumber) ?: store.findEntry(rawNumber)

            diagnosticsStore.addEvent(
                source = "CallScreeningService",
                rawNumber = rawNumber,
                matched = entry != null,
                reason = if (entry != null) "matched" else "not_found"
            )

            if (entry != null) {
                val helper = AlertNotificationHelper(this)
                helper.ensureChannel()
                helper.showCallAlert(rawNumber, entry)
            }

            respondAllow(callDetails)
        } catch (e: Exception) {
            Log.e("ScamCallScreening", "onScreenCall failed", e)
            runCatching { respondAllow(callDetails) }
        }
    }

    private fun respondAllow(callDetails: Call.Details) {
        val builder = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            builder.setSilenceCall(false)
        }

        val response = builder.build()
        respondToCall(callDetails, response)
    }
}
