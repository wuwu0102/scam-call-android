package com.alertanumero.mx.telephony

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.CallScreeningService.CallResponse
import android.util.Log
import com.alertanumero.mx.data.repository.ScamRepository

class ScamCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val startedAt = System.currentTimeMillis()
        Log.i("ScamCallScreening", "onScreenCall entered")
        var responded = false
        try {
            val isAtLeastQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
            val isAtLeastR = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
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
            val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
            Log.i("ScamCallScreening", "rawNumber blank=${rawNumber.isBlank()} direction=$callDirection scheme=$handleScheme")
            val normalizedNumber = ScamRepository().normalizePhone(rawNumber).orEmpty()
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

            Log.i("ScamCallScreening", "respondAllow elapsed=${System.currentTimeMillis() - startedAt}ms")
            respondAllow(callDetails)
            responded = true

            if (!isIncoming) {
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
                return
            }

            val entry = store.findLocalTestEntry(rawNumber) ?: store.findEntry(rawNumber)
            Log.i(
                "ScamCallScreening",
                "lookup finished matched=${entry != null} elapsed=${System.currentTimeMillis() - startedAt}ms"
            )

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
                Log.i("ScamCallScreening", "showCallAlert source=CallScreeningService")
                helper.showCallAlert(rawNumber, entry)
            }
        } catch (e: Exception) {
            Log.e("ScamCallScreening", "onScreenCall failed elapsed=${System.currentTimeMillis() - startedAt}ms", e)
            if (!responded) {
                Log.i("ScamCallScreening", "respondAllow elapsed=${System.currentTimeMillis() - startedAt}ms")
                runCatching { respondAllow(callDetails) }
            }
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
