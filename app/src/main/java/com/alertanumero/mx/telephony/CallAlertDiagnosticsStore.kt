package com.alertanumero.mx.telephony

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LastCallAlertEvent(
    val source: String,
    val rawNumber: String,
    val normalizedNumber: String,
    val matched: Boolean,
    val category: String,
    val reason: String,
    val timestamp: Long,
    val callDirection: String = "",
    val handleScheme: String = "",
    val handlePresentation: String = "",
    val isIncoming: Boolean = true
)

class CallAlertDiagnosticsStore(context: Context) {
    private val prefs = context.getSharedPreferences("call_alert_diagnostics", 0)

    fun addEvent(
        source: String,
        rawNumber: String,
        normalizedNumber: String = "",
        matched: Boolean,
        category: String = "",
        reason: String,
        callDirection: String = "",
        handleScheme: String = "",
        handlePresentation: String = "",
        isIncoming: Boolean = true
    ) {
        val current = readEvents().toMutableList()
        current.add(
            0,
            LastCallAlertEvent(
                source = source,
                rawNumber = rawNumber,
                normalizedNumber = normalizedNumber,
                matched = matched,
                category = category,
                reason = reason,
                timestamp = System.currentTimeMillis(),
                callDirection = callDirection,
                handleScheme = handleScheme,
                handlePresentation = handlePresentation,
                isIncoming = isIncoming
            )
        )
        val limited = current.take(10)
        val array = JSONArray()
        limited.forEach { event ->
            array.put(
                JSONObject()
                    .put("source", event.source)
                    .put("rawNumber", event.rawNumber)
                     .put("normalizedNumber", event.normalizedNumber)
                    .put("matched", event.matched)
                    .put("category", event.category)
                    .put("reason", event.reason)
                    .put("timestamp", event.timestamp)
                    .put("callDirection", event.callDirection)
                    .put("handleScheme", event.handleScheme)
                    .put("handlePresentation", event.handlePresentation)
                    .put("isIncoming", event.isIncoming)
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
                        normalizedNumber = item.optString("normalizedNumber"),
                        matched = item.optBoolean("matched", false),
                        category = item.optString("category"),
                        reason = item.optString("reason"),
                        timestamp = item.optLong("timestamp", 0L),
                        callDirection = item.optString("callDirection"),
                        handleScheme = item.optString("handleScheme"),
                        handlePresentation = item.optString("handlePresentation"),
                        isIncoming = item.optBoolean("isIncoming", true)
                    )
                )
            }
        }
    }
}
