package com.lunchyum.neiswallpaper

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

internal data class Lesson(val period: Int, val subject: String)
internal data class Meal(val type: String, val items: List<String>, val kcal: String?)

internal class NeisClient(private val apiKey: String) {
    private val base = "https://open.neis.go.kr/hub/"

    fun timetable(endpoint: String, office: String, schoolCode: String, grade: String, classNo: String, ymd: String): List<Lesson> {
        val year = ymd.take(4)
        val month = ymd.substring(4, 6).toInt()
        val semester = if (month <= 8) "1" else "2"
        val query = linkedMapOf(
            "KEY" to apiKey, "Type" to "json", "pIndex" to "1", "pSize" to "100",
            "ATPT_OFCDC_SC_CODE" to office, "SD_SCHUL_CODE" to schoolCode,
            "AY" to year, "SEM" to semester, "GRADE" to grade, "CLASS_NM" to classNo,
            "TI_FROM_YMD" to ymd, "TI_TO_YMD" to ymd
        )
        val rows = requestRows(endpoint, query)
        return rows.mapNotNull { row ->
            val period = row.optString("PERIO", row.optString("ALL_TI_YMD", "")).filter(Char::isDigit).toIntOrNull()
            val subject = row.optString("ITRT_CNTNT").cleanHtml()
            if (period != null && subject.isNotBlank()) Lesson(period, subject) else null
        }.sortedBy { it.period }
    }

    fun meals(office: String, schoolCode: String, ymd: String): List<Meal> {
        val query = linkedMapOf(
            "KEY" to apiKey, "Type" to "json", "pIndex" to "1", "pSize" to "100",
            "ATPT_OFCDC_SC_CODE" to office, "SD_SCHUL_CODE" to schoolCode, "MLSV_YMD" to ymd
        )
        return requestRows("mealServiceDietInfo", query).map { row ->
            val items = row.optString("DDISH_NM").cleanHtml().split("\n").map { it.trim() }.filter { it.isNotBlank() }
            Meal(row.optString("MMEAL_SC_NM", "급식"), items, row.optString("CAL_INFO").takeIf { it.isNotBlank() })
        }
    }

    private fun requestRows(endpoint: String, query: Map<String, String>): List<JSONObject> {
        val url = base + endpoint + "?" + query.entries.joinToString("&") {
            URLEncoder.encode(it.key, Charsets.UTF_8.name()) + "=" + URLEncoder.encode(it.value, Charsets.UTF_8.name())
        }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
        }
        conn.connect()
        val code = conn.responseCode
        val body = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
        conn.disconnect()
        if (code !in 200..299) error("나이스 서버 오류 ($code)")
        val root = JSONObject(body)
        val result = root.optJSONObject("RESULT")
        if (result != null) error("${result.optString("MESSAGE", "나이스 API 오류")} [${result.optString("CODE", "UNKNOWN")}]")
        val section = root.keys().asSequence().firstOrNull { it != "RESULT" } ?: return emptyList()
        val array = root.optJSONArray(section) ?: return emptyList()
        val rows = if (array.length() > 1) array.optJSONObject(1)?.optJSONArray("row") else null
        return rows?.toObjectList() ?: emptyList()
    }

    private fun JSONArray.toObjectList(): List<JSONObject> = buildList {
        for (i in 0 until length()) optJSONObject(i)?.let(::add)
    }

    private fun String.cleanHtml(): String = replace("<br/>", "\n").replace("<br />", "\n").replace("&amp;", "&").trim()
}

internal data class WallpaperLesson(val period: Int, val subject: String)
internal data class WallpaperMeal(val type: String, val items: List<String>, val kcal: String?)
internal data class WallpaperSnapshot(
    val schoolName: String,
    val date: String,
    val mode: String,
    val lessons: List<WallpaperLesson>,
    val meals: List<WallpaperMeal>
) {
    fun toJson(): String = JSONObject().apply {
        put("schoolName", schoolName); put("date", date); put("mode", mode)
        put("lessons", JSONArray().apply { lessons.forEach { put(JSONObject().apply { put("period", it.period); put("subject", it.subject) }) } })
        put("meals", JSONArray().apply { meals.forEach { put(JSONObject().apply { put("type", it.type); put("items", JSONArray(it.items)); put("kcal", it.kcal ?: JSONObject.NULL) }) } })
    }.toString()

    companion object {
        fun fromJson(raw: String): WallpaperSnapshot {
            val root = JSONObject(raw)
            val lessons = root.optJSONArray("lessons").let { array -> buildList { if (array != null) for (i in 0 until array.length()) array.optJSONObject(i)?.let { add(WallpaperLesson(it.optInt("period"), it.optString("subject"))) } } }
            val meals = root.optJSONArray("meals").let { array -> buildList {
                if (array != null) for (i in 0 until array.length()) array.optJSONObject(i)?.let { item ->
                    val foods = item.optJSONArray("items")?.let { foodsArray -> buildList { for (j in 0 until foodsArray.length()) add(foodsArray.optString(j)) } } ?: emptyList()
                    add(WallpaperMeal(item.optString("type"), foods, item.optString("kcal").takeIf { it.isNotBlank() && it != "null" }))
                }
            } }
            return WallpaperSnapshot(root.optString("schoolName", "우리 학교"), root.optString("date"), root.optString("mode", "BOTH"), lessons, meals)
        }
    }
}

internal object WallpaperStorage {
    private const val PREFS = "neis_wallpaper"
    private const val KEY = "snapshot"
    fun save(context: android.content.Context, snapshot: WallpaperSnapshot) = context.getSharedPreferences(PREFS, 0).edit().putString(KEY, snapshot.toJson()).apply()
    fun load(context: android.content.Context): WallpaperSnapshot? = context.getSharedPreferences(PREFS, 0).getString(KEY, null)?.let { runCatching { WallpaperSnapshot.fromJson(it) }.getOrNull() }
}
