package com.roproduction.radio.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Клиент открытого каталога радиостанций https://www.radio-browser.info (без рекламы и ключей).
 */
object RadioBrowser {
    private val servers = listOf(
        "de1.api.radio-browser.info",
        "de2.api.radio-browser.info",
        "fi1.api.radio-browser.info",
        "all.api.radio-browser.info",
    )

    /**
     * Ищет станции. Можно указать название, регион (state, например "Tomsk") или тег ("news").
     */
    suspend fun search(
        name: String? = null,
        state: String? = null,
        tag: String? = null,
        countryCode: String? = "RU",
        limit: Int = 50,
    ): List<Station> = withContext(Dispatchers.IO) {
        val params = buildList {
            name?.takeIf { it.isNotBlank() }?.let { add("name=" + enc(it.trim())) }
            state?.let { add("state=" + enc(it)) }
            tag?.let { add("tag=" + enc(it)) }
            countryCode?.let { add("countrycode=$it") }
            add("hidebroken=true")
            add("order=clickcount")
            add("reverse=true")
            add("limit=$limit")
        }.joinToString("&")

        var lastError: Exception? = null
        for (host in servers) {
            try {
                return@withContext parse(get("https://$host/json/stations/search?$params"))
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Каталог недоступен")
    }

    /** Возвращает ссылки на потоки для запросов, по порядку — от наиболее подходящих. */
    suspend fun findStreamUrls(queries: List<String>): List<String> {
        val result = LinkedHashSet<String>()
        for (q in queries) {
            val found = runCatching { search(name = q, countryCode = null, limit = 10) }.getOrDefault(emptyList())
            // Сначала станции, чьё название совпадает с запросом точнее всего.
            found.sortedByDescending { score(it.name, q) }
                .filter { score(it.name, q) > 0 }
                .forEach { result += it.streamUrl }
            if (result.size >= 3) break
        }
        return result.toList()
    }

    private fun score(stationName: String, query: String): Int {
        val a = stationName.lowercase().replace("ё", "е")
        val b = query.lowercase().replace("ё", "е")
        return when {
            a == b -> 3
            a.startsWith(b) -> 2
            a.contains(b) -> 1
            else -> 0
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 10000
        conn.setRequestProperty("User-Agent", "RadioApp/1.0 (Android)")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: String): List<Station> {
        val arr = JSONArray(json)
        val seen = HashSet<String>()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val url = o.optString("url_resolved").ifBlank { o.optString("url") }
                if (url.isBlank() || !seen.add(url)) continue
                val name = o.optString("name").trim()
                if (name.isBlank()) continue
                val info = listOfNotNull(
                    o.optString("state").takeIf { it.isNotBlank() },
                    o.optString("tags").split(',').map { it.trim() }.filter { it.isNotBlank() }
                        .take(3).joinToString(", ").takeIf { it.isNotBlank() },
                    o.optInt("bitrate").takeIf { it > 0 }?.let { "$it kbps" },
                ).joinToString(" · ")
                add(
                    Station(
                        id = "rb:" + o.optString("stationuuid", url),
                        name = name,
                        description = info,
                        streamUrl = url,
                        searchQueries = listOf(name),
                        isCustom = true,
                    )
                )
            }
        }
    }
}
