package com.alertanumero.mx.telephony

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.util.Log
import com.alertanumero.mx.data.repository.ScamRepository

class ScamCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        try {
            val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            val isAtLeastR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
            val normalizedNumber = ScamRepository().normalizePhone(rawNumber).orEmpty()
            val callDirectionValue = if (isAtLeastQ) {
                callDetails.callDirection
            } else {
                Call.Details.DIRECTION_INCOMING
            }
            val callDirection = if (isAtLeastQ) {
                when (callDirectionValue) {
                    Call.Details.DIRECTION_INCOMING -> "INCOMING"
                    Call.Details.DIRECTION_OUTGOING -> "OUTGOING"
                    else -> "UNKNOWN"
                }
            } else {
                "ASSUMED_INCOMING_PRE_Q"
            }
            val handleScheme = callDetails.handle?.scheme.orEmpty()
            val handlePresentation = if (isAtLeastR) {
                callDetails.callerNumberVerificationStatus.toString()
            } else {
                "N/A_PRE_R"
            }
            val isIncoming = if (isAtLeastQ) {
                callDirectionValue == Call.Details.DIRECTION_INCOMING
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
            val diagnosticModeEnabled = store.isDiagnosticNotifyAllCallsEnabled()
            val helper = AlertNotificationHelper(this).also { it.ensureChannel() }
            if (diagnosticModeEnabled) {
                helper.showDiagnosticCallSeen(rawNumber, "CallScreeningService")
            }

            if (rawNumber.isBlank()) {
                if (diagnosticModeEnabled) {
                    helper.showDiagnosticCallSeen(
                        "CallScreeningService activo, pero Android no entregó el número",
                        "CallScreeningService"
                    )
                }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setSilenceCall(false)
        }

        val response = builder.build()
        respondToCall(callDetails, response)
    }
}
