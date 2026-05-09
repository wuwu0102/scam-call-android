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

data class ScamDatabaseSnapshot(
    val numbers: Set<String>,
    val totalCount: Int,
    val updatedAt: String,
    val sourceUrl: String
)

sealed class FetchResult {
    data class Success(val snapshot: ScamDatabaseSnapshot) : FetchResult()
    data class Error(val message: String) : FetchResult()
}

class ScamRepository {
    companion object {
        private const val TAG = "ScamRepository"
    }

    private val fallbackUrls = listOf(
        "https://alertanumero.github.io/scam-call-database/scam-database-mx.json",
        "https://raw.githubusercontent.com/alertanumero/scam-call-database/main/docs/scam-database-mx.json"
    )

    private val service: ScamDatabaseService by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ScamDatabaseService::class.java)
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
        val numbers = linkedSetOf<String>()
        var updatedAt = "N/A"

        if (raw.trim().startsWith("[")) {
            val array = JSONArray(raw)
            collectFromArray(array, numbers)
        } else {
            val obj = JSONObject(raw)
            updatedAt = obj.optString("updated_at").ifBlank {
                obj.optString("last_updated").ifBlank {
                    obj.optString("generated_at").ifBlank { "N/A" }
                }
            }

            obj.optJSONArray("records")?.let { collectFromArray(it, numbers) }
            obj.optJSONArray("data")?.let { collectFromArray(it, numbers) }
            obj.optJSONArray("numbers")?.let { collectFromArray(it, numbers) }
            obj.optJSONArray("entries")?.let { collectFromArray(it, numbers) }

            if (numbers.isEmpty()) {
                collectFromObjectValues(obj, numbers)
            }
        }

        if (updatedAt == "N/A") {
            updatedAt = DateTimeFormatter.ISO_INSTANT
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
        }

        return ScamDatabaseSnapshot(
            numbers = numbers,
            totalCount = numbers.size,
            updatedAt = updatedAt,
            sourceUrl = sourceUrl
        )
    }

    private fun collectFromArray(array: JSONArray, target: MutableSet<String>) {
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is String -> normalizePhone(item)?.let(target::add)
                is JSONObject -> {
                    val rawPhone = item.optString("phone").ifBlank {
                        item.optString("number").ifBlank {
                            item.optString("telefono").ifBlank {
                                item.optString("phone_number")
                            }
                        }
                    }
                    normalizePhone(rawPhone)?.let(target::add)
                }
            }
        }
    }

    private fun collectFromObjectValues(obj: JSONObject, target: MutableSet<String>) {
        obj.keys().forEach { key ->
            val value = obj.opt(key)
            when (value) {
                is String -> normalizePhone(value)?.let(target::add)
                is JSONArray -> collectFromArray(value, target)
            }
        }
    }

    fun normalizePhone(input: String): String? {
        val digitsOnly = input.replace(Regex("[^0-9+]"), "")
        if (digitsOnly.isBlank()) return null

        val withoutPlus = digitsOnly.removePrefix("+")
        val mxStripped = when {
            withoutPlus.startsWith("52") && withoutPlus.length > 10 -> withoutPlus.removePrefix("52")
            else -> withoutPlus
        }

        val last10 = if (mxStripped.length >= 10) mxStripped.takeLast(10) else mxStripped
        return last10.takeIf { it.length == 10 }
    }
}
