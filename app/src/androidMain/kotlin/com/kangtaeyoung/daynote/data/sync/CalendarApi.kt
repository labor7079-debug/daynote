package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** 인증 만료(401) 등 호출 실패를 구분하기 위한 예외. */
class CalendarApiException(val code: Int, message: String) : Exception(message)

/** calendarList 항목 — 내 캘린더 + 공유받은 캘린더. */
data class RemoteCalendar(val id: String, val name: String, val colorHex: String?, val primary: Boolean)

/** 다른 캘린더에서 읽어온 이벤트 한 건(단일 인스턴스로 펼친 상태). */
data class RemoteEvent(
    val id: String,
    val title: String,
    val startMillis: Long,
    /** 종료(포함). 하루짜리 종일 이벤트는 null. */
    val endMillis: Long?,
    val allDay: Boolean,
)

/**
 * 구글 Calendar v3 REST 최소 클라이언트(Android 전용). Android 내장 [HttpURLConnection] + `org.json`
 * 만 사용해 새 의존성 없이 동작한다. 모든 호출은 IO 디스패처에서 블로킹 실행한다.
 *
 * [allDay]=true → 종일 이벤트(start.date ~ 다음날 end.date),
 * [allDay]=false → 시간 이벤트(start.dateTime ~ +1시간 end.dateTime, RFC3339 UTC).
 */
class CalendarApi(
    private val calendarId: String = "primary",
) {
    private val base = "https://www.googleapis.com/calendar/v3/calendars/$calendarId/events"

    suspend fun insertEvent(token: String, summary: String, description: String, startMillis: Long, allDay: Boolean): String =
        withContext(Dispatchers.IO) {
            request("POST", base, token, eventBody(summary, description, startMillis, allDay)).getString("id")
        }

    suspend fun updateEvent(token: String, eventId: String, summary: String, description: String, startMillis: Long, allDay: Boolean) =
        withContext(Dispatchers.IO) {
            request("PUT", "$base/$eventId", token, eventBody(summary, description, startMillis, allDay))
            Unit
        }

    /** 이벤트 삭제. 이미 없으면(404/410) 성공으로 간주. */
    suspend fun deleteEvent(token: String, eventId: String) = withContext(Dispatchers.IO) {
        try {
            request("DELETE", "$base/$eventId", token, null)
        } catch (e: CalendarApiException) {
            if (e.code != 404 && e.code != 410) throw e
        }
        Unit
    }

    /** 계정의 캘린더 목록(공유받은 캘린더 포함). `calendar.calendarlist.readonly` 범위 필요. */
    suspend fun listCalendars(token: String): List<RemoteCalendar> = withContext(Dispatchers.IO) {
        val res = request(
            "GET",
            "https://www.googleapis.com/calendar/v3/users/me/calendarList?maxResults=250" +
                "&fields=items(id,summary,summaryOverride,backgroundColor,primary)",
            token,
            null,
        )
        val items = res.optJSONArray("items") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until items.length()) {
                val cal = items.getJSONObject(i)
                add(
                    RemoteCalendar(
                        id = cal.getString("id"),
                        // 공유 캘린더에 사용자가 붙인 별칭(summaryOverride)이 있으면 우선.
                        name = cal.optString("summaryOverride").ifBlank { cal.optString("summary") }
                            .ifBlank { cal.getString("id") },
                        colorHex = cal.optString("backgroundColor").takeIf { it.isNotBlank() },
                        primary = cal.optBoolean("primary", false),
                    ),
                )
            }
        }
    }

    /**
     * [calendarId] 캘린더의 [timeMinMillis, timeMaxMillis) 이벤트 목록.
     * 반복 일정은 단일 인스턴스로 펼친다(singleEvents). 한 창(window)당 최대 2500건 — 개인 캘린더엔 충분.
     */
    suspend fun listEvents(token: String, calendarId: String, timeMinMillis: Long, timeMaxMillis: Long): List<RemoteEvent> =
        withContext(Dispatchers.IO) {
            val encodedId = URLEncoder.encode(calendarId, "UTF-8")
            val timeMin = Instant.fromEpochMilliseconds(timeMinMillis).toString()
            val timeMax = Instant.fromEpochMilliseconds(timeMaxMillis).toString()
            val res = request(
                "GET",
                "https://www.googleapis.com/calendar/v3/calendars/$encodedId/events" +
                    "?singleEvents=true&orderBy=startTime&maxResults=2500" +
                    "&timeMin=$timeMin&timeMax=$timeMax" +
                    "&fields=items(id,status,summary,start,end)",
                token,
                null,
            )
            val items = res.optJSONArray("items") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until items.length()) {
                    val ev = items.getJSONObject(i)
                    if (ev.optString("status") == "cancelled") continue
                    val start = ev.optJSONObject("start") ?: continue
                    val end = ev.optJSONObject("end")
                    val startDate = start.optString("date").takeIf { it.isNotBlank() }
                    if (startDate != null) {
                        // 종일 이벤트 — end.date 는 exclusive(다음날) → 포함 종료일은 하루 앞.
                        val s = LocalDate.parse(startDate)
                        val endExclusive = end?.optString("date")?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
                        val lastDay = endExclusive?.minus(1, DateTimeUnit.DAY)?.takeIf { it > s }
                        add(
                            RemoteEvent(
                                id = ev.getString("id"),
                                title = ev.optString("summary").ifBlank { "(제목 없음)" },
                                startMillis = s.startOfDayMillis(),
                                endMillis = lastDay?.startOfDayMillis(),
                                allDay = true,
                            ),
                        )
                    } else {
                        val startDateTime = start.optString("dateTime").takeIf { it.isNotBlank() } ?: continue
                        val sMillis = Instant.parse(startDateTime).toEpochMilliseconds()
                        val eMillis = end?.optString("dateTime")?.takeIf { it.isNotBlank() }
                            ?.let { Instant.parse(it).toEpochMilliseconds() }
                            ?.takeIf { it > sMillis }
                        add(
                            RemoteEvent(
                                id = ev.getString("id"),
                                title = ev.optString("summary").ifBlank { "(제목 없음)" },
                                startMillis = sMillis,
                                endMillis = eMillis,
                                allDay = false,
                            ),
                        )
                    }
                }
            }
        }

    private fun eventBody(summary: String, description: String, startMillis: Long, allDay: Boolean): String {
        val json = JSONObject().apply {
            put("summary", summary.ifBlank { "(제목 없음)" })
            if (description.isNotBlank()) put("description", description)
        }
        if (allDay) {
            val date = startMillis.toLocalDate()
            val end = date.plus(1, DateTimeUnit.DAY)
            json.put("start", JSONObject().put("date", date.toString()))
            json.put("end", JSONObject().put("date", end.toString()))
        } else {
            val start = Instant.fromEpochMilliseconds(startMillis)
            val end = start.plus(1, DateTimeUnit.HOUR)
            json.put("start", JSONObject().put("dateTime", start.toString())) // RFC3339, e.g. 2026-06-30T05:00:00Z
            json.put("end", JSONObject().put("dateTime", end.toString()))
        }
        return json.toString()
    }

    private fun request(method: String, urlStr: String, token: String, body: String?): JSONObject {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15_000
            readTimeout = 15_000
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) {
                throw CalendarApiException(code, "Calendar API $code: ${text.take(300)}")
            }
            return if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
