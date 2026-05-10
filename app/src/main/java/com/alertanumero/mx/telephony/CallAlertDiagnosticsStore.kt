package com.alertanumero.mx.telephony

import android.content.Context

data class LastCallAlertEvent(
    val source: String,
    val rawNumber: String,
    val matched: Boolean,
    val reason: String,
    val timestamp: Long
)

class CallAlertDiagnosticsStore(context: Context) {
    private val prefs = context.getSharedPreferences("call_alert_diagnostics", 0)

    fun saveLastEvent(
        source: String,
        rawNumber: String,
        matched: Boolean,
        reason: String
    ) {
        prefs.edit()
            .putString("source", source)
            .putString("rawNumber", rawNumber)
            .putBoolean("matched", matched)
            .putString("reason", reason)
            .putLong("timestamp", System.currentTimeMillis())
            .apply()
    }

    fun getLastEvent(): LastCallAlertEvent? {
        val timestamp = prefs.getLong("timestamp", 0L)
        if (timestamp <= 0L) return null
        return LastCallAlertEvent(
            source = prefs.getString("source", "").orEmpty(),
            rawNumber = prefs.getString("rawNumber", "").orEmpty(),
            matched = prefs.getBoolean("matched", false),
            reason = prefs.getString("reason", "").orEmpty(),
            timestamp = timestamp
        )
    }
}
