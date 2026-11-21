package com.roproduction.radio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Хранит избранное, свои станции, исправленные ссылки и настройки в SharedPreferences. */
class StationRepository(context: Context) {
    private val prefs = context.getSharedPreferences("radio", Context.MODE_PRIVATE)

    /** Избранное в порядке добавления. */
    fun favorites(): List<String> {
        val arr = JSONArray(prefs.getString(KEY_FAVORITES, "[]"))
        return List(arr.length()) { arr.getString(it) }
    }

    fun saveFavorites(ids: List<String>) {
        prefs.edit().putString(KEY_FAVORITES, JSONArray(ids).toString()).apply()
    }

    fun customStations(): List<Station> {
        val arr = JSONArray(prefs.getString(KEY_CUSTOM, "[]"))
        return List(arr.length()) { Station.fromJson(arr.getJSONObject(it)) }
    }

    fun saveCustomStations(list: List<Station>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_CUSTOM, arr.toString()).apply()
    }

    /** Ссылки, которые пользователь исправил вручную или которые нашлись автоматически. */
    fun urlOverrides(): Map<String, String> {
        val o = JSONObject(prefs.getString(KEY_OVERRIDES, "{}")!!)
        return o.keys().asSequence().associateWith { o.getString(it) }
    }

    fun setUrlOverride(stationId: String, url: String?) {
        val o = JSONObject(prefs.getString(KEY_OVERRIDES, "{}")!!)
        if (url.isNullOrBlank()) o.remove(stationId) else o.put(stationId, url)
        prefs.edit().putString(KEY_OVERRIDES, o.toString()).apply()
    }

    var lastStationId: String?
        get() = prefs.getString(KEY_LAST, null)
        set(value) = prefs.edit().putString(KEY_LAST, value).apply()

    var lastTimerMinutes: Int
        get() = prefs.getInt(KEY_TIMER, 30)
        set(value) = prefs.edit().putInt(KEY_TIMER, value).apply()

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_CUSTOM = "custom_stations"
        const val KEY_OVERRIDES = "url_overrides"
        const val KEY_LAST = "last_station"
        const val KEY_TIMER = "last_timer_minutes"
    }
}
