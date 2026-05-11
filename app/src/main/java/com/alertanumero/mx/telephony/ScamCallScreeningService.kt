package com.alertanumero.mx.telephony

import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.util.Log
import com.alertanumero.mx.data.repository.ScamRepository

class ScamCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        try {
            val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
            val normalizedNumber = ScamRepository().normalizePhone(rawNumber).orEmpty()
            val callDirection = when (callDetails.callDirection) {
                Call.Details.DIRECTION_INCOMING -> "INCOMING"
                Call.Details.DIRECTION_OUTGOING -> "OUTGOING"
                else -> "UNKNOWN"
            }
            val handleScheme = callDetails.handle?.scheme.orEmpty()
            val handlePresentation = callDetails.callerNumberVerificationStatus.toString()
            val isIncoming = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                callDetails.callDirection == Call.Details.DIRECTION_INCOMING
            } else {
                true
            }
            val diagnosticsStore = CallAlertDiagnosticsStore(this)
            diagnosticsStore.addEvent(
                source = "CallScreeningService",
                rawNumber = rawNumber,
                normalizedNumber = normalizedNumber,
                matched = false,
                reason = "onScreenCall_entered",
                callDirection = callDirection,
                handleScheme = handleScheme,
                handlePresentation = handlePresentation,
                isIncoming = isIncoming
            )

            if (!isIncoming) {
                respondAllow(callDetails)
                return
            }

            val store = CallAlertStore(this)
            if (store.isDiagnosticNotifyAllCallsEnabled()) {
                val helper = AlertNotificationHelper(this)
                helper.ensureChannel()
                helper.showDiagnosticCallSeen(rawNumber, "CallScreeningService")
                CallerIdOverlayActivity.start(
                    context = this,
                    rawNumber = rawNumber,
                    label = "Diagnóstico: llamada detectada",
                    category = "DIAGNOSTIC",
                    source = "CallScreeningService"
                )
            }

            if (rawNumber.isBlank()) {
                diagnosticsStore.addEvent(
                    source = "CallScreeningService",
                    rawNumber = "",
                    normalizedNumber = normalizedNumber,
                    matched = false,
                    reason = "empty_or_unavailable_number",
                    callDirection = callDirection,
                    handleScheme = handleScheme,
                    handlePresentation = handlePresentation,
                    isIncoming = isIncoming
                )
                respondAllow(callDetails)
                return
            }

            val entry = store.findLocalTestEntry(rawNumber) ?: store.findEntry(rawNumber)

            diagnosticsStore.addEvent(
                source = "CallScreeningService",
                rawNumber = rawNumber,
                normalizedNumber = normalizedNumber,
                matched = entry != null,
                category = entry?.category?.name.orEmpty(),
                reason = if (entry != null) "matched" else "not_found",
                callDirection = callDirection,
                handleScheme = handleScheme,
                handlePresentation = handlePresentation,
                isIncoming = isIncoming
            )

            if (entry != null) {
                val helper = AlertNotificationHelper(this)
                helper.ensureChannel()
                helper.showCallAlert(rawNumber, entry)
                CallerIdOverlayActivity.start(
                    context = this,
                    rawNumber = rawNumber,
                    label = entry.label,
                    category = entry.category.name,
                    source = "CallScreeningService"
                )
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
