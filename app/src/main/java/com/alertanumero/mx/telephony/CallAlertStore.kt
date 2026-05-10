package com.alertanumero.mx.telephony

import android.content.Context
import com.alertanumero.mx.data.repository.ScamCategory
import com.alertanumero.mx.data.repository.ScamEntry
import com.alertanumero.mx.data.repository.ScamRepository
import org.json.JSONArray
import org.json.JSONObject

class CallAlertStore(context: Context) {
    private val prefs = context.getSharedPreferences("call_alert_store", 0)
    private val repository = ScamRepository()

    fun saveEntries(entries: Map<String, ScamEntry>) {
        val array = JSONArray()
        entries.values.forEach { entry ->
            array.put(
                JSONObject()
                    .put("number", entry.number)
                    .put("category", entry.category.name)
                    .put("label", entry.label)
                    .put("source", entry.source)
            )
        }
        prefs.edit().putString("cached_entries_json", array.toString()).apply()
    }

    fun findEntry(rawNumber: String): ScamEntry? {
        val normalized = repository.normalizePhone(rawNumber) ?: return null
        val raw = prefs.getString("cached_entries_json", null) ?: return null
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return null
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            if (item.optString("number") != normalized) continue
            val category = runCatching { ScamCategory.valueOf(item.optString("category")) }.getOrDefault(ScamCategory.SUSPICIOUS)
            val label = item.optString("label").ifBlank { category.displayLabel }
            return ScamEntry(normalized, category, label, item.optString("source"))
        }
        return null
    }


    fun setDiagnosticNotifyAllCalls(enabled: Boolean) {
        prefs.edit().putBoolean("diagnostic_notify_all_calls", enabled).apply()
    }

    fun isDiagnosticNotifyAllCallsEnabled(): Boolean {
        return prefs.getBoolean("diagnostic_notify_all_calls", false)
    }

    fun saveNumbers(numbers: Set<String>) {
        saveEntries(numbers.associateWith {
            ScamEntry(it, ScamCategory.SUSPICIOUS, ScamCategory.SUSPICIOUS.displayLabel)
        })
    }

    fun hasSuspiciousNumber(rawNumber: String): Boolean = findEntry(rawNumber) != null

    fun saveLocalTestNumber(number: String) {
        prefs.edit().putString("local_test_number", number).apply()
    }

    fun findLocalTestEntry(rawNumber: String): ScamEntry? {
        val normalized = repository.normalizePhone(rawNumber) ?: return null
        val local = prefs.getString("local_test_number", null) ?: return null
        if (local != normalized) return null
        return ScamEntry(normalized, ScamCategory.SUSPICIOUS, "Prueba local", "local")
    }

    fun isLocalTestHit(rawNumber: String): Boolean = findLocalTestEntry(rawNumber) != null
}
