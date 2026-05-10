package com.alertanumero.mx.telephony

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LastCallAlertEvent(
    val source: String,
    val rawNumber: String,
    val matched: Boolean,
    val reason: String,
    val timestamp: Long
)

class CallAlertDiagnosticsStore(context: Context) {
    private val prefs = context.getSharedPreferences("call_alert_diagnostics", 0)

    fun addEvent(
        source: String,
        rawNumber: String,
        matched: Boolean,
        reason: String
    ) {
        val current = readEvents().toMutableList()
        current.add(
            0,
            LastCallAlertEvent(
                source = source,
                rawNumber = rawNumber,
                matched = matched,
                reason = reason,
                timestamp = System.currentTimeMillis()
            )
        )
        val limited = current.take(10)
        val array = JSONArray()
        limited.forEach { event ->
            array.put(
                JSONObject()
                    .put("source", event.source)
                    .put("rawNumber", event.rawNumber)
                    .put("matched", event.matched)
                    .put("reason", event.reason)
                    .put("timestamp", event.timestamp)
            )
        }
        prefs.edit().putString("call_alert_events_json", array.toString()).apply()
    }

    fun getLastEvent(): LastCallAlertEvent? = getRecentEvents().firstOrNull()

    fun getRecentEvents(): List<LastCallAlertEvent> = readEvents()

    private fun readEvents(): List<LastCallAlertEvent> {
        val raw = prefs.getString("call_alert_events_json", null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    LastCallAlertEvent(
                        source = item.optString("source"),
                        rawNumber = item.optString("rawNumber"),
                        matched = item.optBoolean("matched", false),
                        reason = item.optString("reason"),
                        timestamp = item.optLong("timestamp", 0L)
                    )
                )
            }
        }
    }
}
