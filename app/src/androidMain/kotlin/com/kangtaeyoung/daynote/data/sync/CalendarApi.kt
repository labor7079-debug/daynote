package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/** 인증 만료(401) 등 호출 실패를 구분하기 위한 예외. */
class CalendarApiException(val code: Int, message: String) : Exception(message)

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
