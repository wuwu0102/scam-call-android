package com.alertanumero.mx.telephony

import android.content.Context
import com.alertanumero.mx.data.repository.ScamRepository

class CallAlertStore(context: Context) {
    private val prefs = context.getSharedPreferences("call_alert_store", 0)
    private val repository = ScamRepository()

    fun saveNumbers(numbers: Set<String>) {
        prefs.edit().putStringSet("cached_numbers", numbers).apply()
    }

    fun hasSuspiciousNumber(rawNumber: String): Boolean {
        val normalized = repository.normalizePhone(rawNumber) ?: return false
        return prefs.getStringSet("cached_numbers", emptySet()).orEmpty().contains(normalized)
    }

    fun saveLocalTestNumber(number: String) {
        prefs.edit().putString("local_test_number", number).apply()
    }

    fun isLocalTestHit(rawNumber: String): Boolean {
        val normalized = repository.normalizePhone(rawNumber) ?: return false
        val local = prefs.getString("local_test_number", null) ?: return false
        return local == normalized
    }
}
