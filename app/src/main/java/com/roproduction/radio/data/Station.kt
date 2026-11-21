package com.roproduction.radio.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Радиостанция.
 *
 * @param streamUrl прямая ссылка на поток. Может быть пустой — тогда поток ищется в каталоге
 *   radio-browser.info по [searchQueries].
 * @param searchQueries запросы для поиска запасного потока, если прямая ссылка не работает.
 */
data class Station(
    val id: String,
    val name: String,
    val description: String,
    val streamUrl: String,
    val searchQueries: List<String> = listOf(name),
    val isCustom: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("description", description)
        .put("streamUrl", streamUrl)
        .put("searchQueries", JSONArray(searchQueries))

    companion object {
        fun fromJson(o: JSONObject): Station {
            val q = o.optJSONArray("searchQueries")
            val queries = if (q == null) emptyList() else List(q.length()) { q.getString(it) }
            return Station(
                id = o.getString("id"),
                name = o.getString("name"),
                description = o.optString("description"),
                streamUrl = o.optString("streamUrl"),
                searchQueries = queries,
                isCustom = true,
            )
        }
    }
}
