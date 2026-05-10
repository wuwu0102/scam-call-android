package com.alertanumero.mx.data.repository

import android.util.Log
import com.alertanumero.mx.data.remote.ScamDatabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

enum class ScamCategory {
    SUSPICIOUS,
    TELEMARKETING,
    COLLECTION;

    val displayLabel: String
        get() = when (this) {
            SUSPICIOUS -> "Llamada sospechosa"
            TELEMARKETING -> "Publicidad / Telemarketing"
            COLLECTION -> "Cobranza"
        }

    val shortLabel: String
        get() = when (this) {
            SUSPICIOUS -> "Sospechosa"
            TELEMARKETING -> "Publicidad"
            COLLECTION -> "Cobranza"
        }

    companion object {
        fun fromRaw(raw: String?): ScamCategory {
            val value = raw.orEmpty().trim().lowercase()
            return when {
                value.contains("telemarketing") || value.contains("publicidad") || value.contains("advertising") ||
                    value.contains("marketing") || value.contains("promo") || value.contains("ventas") ||
                    value.contains("promoción") || value.contains("promocion") -> TELEMARKETING
                value.contains("collection") || value.contains("cobranza") || value.contains("cobro") ||
                    value.contains("debt") || value.contains("deuda") || value.contains("financiera") -> COLLECTION
                else -> SUSPICIOUS
            }
        }
    }
}

data class ScamEntry(
    val number: String,
    val category: ScamCategory,
    val label: String,
    val source: String = ""
)

data class ScamDatabaseSnapshot(
    val entries: Map<String, ScamEntry>,
    val totalCount: Int,
    val updatedAt: String,
    val sourceUrl: String
) {
    val numbers: Set<String> get() = entries.keys
}

sealed class FetchResult {
    data class Success(val snapshot: ScamDatabaseSnapshot) : FetchResult()
    data class Error(val message: String) : FetchResult()
}

class ScamRepository {
    companion object {
        private const val TAG = "ScamRepository"
    }

    private val fallbackUrls = listOf(
        "https://raw.githubusercontent.com/wuwu0102/scam-call-database/main/data/android_numbers.json",
        "https://raw.githubusercontent.com/wuwu0102/scam-call-database/main/data/scam_numbers.json",
        "https://raw.githubusercontent.com/wuwu0102/scam-call-database/main/scam_numbers.json",
        "https://raw.githubusercontent.com/wuwu0102/scam-call-database/main/data/ios_numbers.json",
        "https://raw.githubusercontent.com/wuwu0102/scam-call-database/main/data/mexico_seed_phone_numbers.json"
    )
    private val service: ScamDatabaseService by lazy {
        val okHttp = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
        Retrofit.Builder().baseUrl("https://example.com/").client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create()).build().create(ScamDatabaseService::class.java)
    }

    suspend fun fetchDatabase(): FetchResult = withContext(Dispatchers.IO) {
        var lastError = "Error desconocido"
        for (url in fallbackUrls) {
            repeat(2) { attempt ->
                try {
                    val response = service.fetchDatabase(url)
                    if (!response.isSuccessful) {
                        lastError = "HTTP ${response.code()}"
                        Log.e(TAG, "fetchDatabase failed url=$url attempt=${attempt + 1} http=${response.code()}")
                        return@repeat
                    }
                    val body = response.body()?.string().orEmpty()
                    if (body.isBlank()) {
                        lastError = "Respuesta vacía"
                        Log.e(TAG, "fetchDatabase empty body url=$url attempt=${attempt + 1}")
                        return@repeat
                    }
                    val snapshot = parseSnapshot(body, url)
                    if (snapshot.entries.isEmpty()) {
                        lastError = "Base de datos vacía"
                        Log.e(TAG, "fetchDatabase parsed empty entries url=$url attempt=${attempt + 1}")
                        return@repeat
                    }
                    Log.i(TAG, "fetchDatabase success url=$url records=${snapshot.totalCount}")
                    return@withContext FetchResult.Success(snapshot)
                } catch (e: Exception) {
                    lastError = e.message ?: "Error de red"
                    Log.e(TAG, "fetchDatabase exception url=$url attempt=${attempt + 1}: ${e.message}", e)
                }
            }
        }
        FetchResult.Error("No se pudo actualizar la base de datos. Error: $lastError")
    }

    private fun parseSnapshot(raw: String, sourceUrl: String): ScamDatabaseSnapshot {
        val entries = linkedMapOf<String, ScamEntry>()
        var updatedAt = "N/A"
        if (raw.trim().startsWith("[")) {
            collectFromArray(JSONArray(raw), entries)
        } else {
            val obj = JSONObject(raw)
            updatedAt = firstNonBlank(obj, listOf("updated_at", "last_updated", "generated_at")).ifBlank { "N/A" }
            obj.optJSONArray("records")?.let { collectFromArray(it, entries) }
            obj.optJSONArray("data")?.let { collectFromArray(it, entries) }
            obj.optJSONArray("numbers")?.let { collectFromArray(it, entries) }
            obj.optJSONArray("entries")?.let { collectFromArray(it, entries) }
            collectFromObjectValues(obj, entries)
        }
        if (updatedAt == "N/A") updatedAt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now())
        return ScamDatabaseSnapshot(entries = entries, totalCount = entries.size, updatedAt = updatedAt, sourceUrl = sourceUrl)
    }

    private fun collectFromArray(array: JSONArray, target: MutableMap<String, ScamEntry>, defaultCategory: ScamCategory = ScamCategory.SUSPICIOUS) {
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is String -> normalizePhone(item)?.let { n -> target[n] = ScamEntry(n, defaultCategory, defaultCategory.displayLabel) }
                is JSONObject -> buildEntryFromObject(item, defaultCategory)?.let { target[it.number] = it }
            }
        }
    }

    private fun collectFromObjectValues(obj: JSONObject, target: MutableMap<String, ScamEntry>) {
        obj.keys().forEach { key ->
            val value = obj.opt(key)
            if (value is JSONArray) {
                val defaultCategory = ScamCategory.fromRaw(key)
                collectFromArray(value, target, defaultCategory)
            }
        }
    }

    private fun buildEntryFromObject(item: JSONObject, defaultCategory: ScamCategory): ScamEntry? {
        val rawPhone = firstNonBlank(item, listOf("phone", "number", "telefono", "phone_number", "tel", "raw_number"))
        val normalized = normalizePhone(rawPhone) ?: return null
        val categoryRaw = firstNonBlank(item, listOf("category", "type", "kind", "group", "riskType", "risk_type"))
        val labelRaw = firstNonBlank(item, listOf("displayLabel", "display_label", "label", "title"))
        val sourceRaw = firstNonBlank(item, listOf("source", "sourceUrl", "source_url", "url", "origin"))
        val category = when {
            categoryRaw.isNotBlank() -> ScamCategory.fromRaw(categoryRaw)
            labelRaw.isNotBlank() -> ScamCategory.fromRaw(labelRaw)
            else -> defaultCategory
        }
        val label = labelRaw.ifBlank { category.displayLabel }
        return ScamEntry(number = normalized, category = category, label = label, source = sourceRaw)
    }

    private fun firstNonBlank(obj: JSONObject, keys: List<String>): String {
        for (key in keys) {
            val value = obj.opt(key)?.toString()?.trim().orEmpty()
            if (value.isNotBlank() && value.lowercase() != "null") return value
        }
        return ""
    }

    fun normalizePhone(input: String): String? {
        val digitsOnly = input.replace(Regex("[^0-9+]"), "")
        if (digitsOnly.isBlank()) return null
        val withoutPlus = digitsOnly.removePrefix("+")
        val mxStripped = if (withoutPlus.startsWith("52") && withoutPlus.length > 10) withoutPlus.removePrefix("52") else withoutPlus
        val last10 = if (mxStripped.length >= 10) mxStripped.takeLast(10) else mxStripped
        return last10.takeIf { it.length == 10 }
    }
}
