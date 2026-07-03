package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.toLocalDate
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/** 인증 만료(401)·권한 부족(403) 등 호출 실패를 구분하기 위한 예외. */
class CalendarApiException(val code: Int, message: String) : Exception(message)

/** 인가 요청 공용 범위 — push(events) + 캘린더 목록 읽기(공유 캘린더 표시). 양 플랫폼 공용. */
val GOOGLE_CALENDAR_SCOPES = listOf(
    "https://www.googleapis.com/auth/calendar.events",
    "https://www.googleapis.com/auth/calendar.calendarlist.readonly",
)

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
 * 구글 Calendar v3 REST 클라이언트 — **멀티플랫폼(Ktor)**. Android·Desktop 이 공유하고,
 * 액세스 토큰 확보(인증)만 플랫폼별 매니저가 담당한다(Android=Play 서비스, Desktop=PKCE 루프백).
 *
 * [allDay]=true → 종일 이벤트(start.date ~ 다음날 end.date),
 * [allDay]=false → 시간 이벤트(start.dateTime ~ +1시간 end.dateTime, RFC3339 UTC).
 */
class GoogleCalendarApi {

    private val client = HttpClient()

    suspend fun insertEvent(token: String, summary: String, description: String, startMillis: Long, allDay: Boolean): String =
        call(HttpMethod.Post, "$BASE/calendars/primary/events", token, eventBody(summary, description, startMillis, allDay))
            .getValue("id").jsonPrimitive.content

    suspend fun updateEvent(token: String, eventId: String, summary: String, description: String, startMillis: Long, allDay: Boolean) {
        call(HttpMethod.Put, "$BASE/calendars/primary/events/$eventId", token, eventBody(summary, description, startMillis, allDay))
    }

    /** 이벤트 삭제. 이미 없으면(404/410) 성공으로 간주. */
    suspend fun deleteEvent(token: String, eventId: String) {
        try {
            call(HttpMethod.Delete, "$BASE/calendars/primary/events/$eventId", token, null)
        } catch (e: CalendarApiException) {
            if (e.code != 404 && e.code != 410) throw e
        }
    }

    /** 계정의 캘린더 목록(공유받은 캘린더 포함). `calendar.calendarlist.readonly` 범위 필요. */
    suspend fun listCalendars(token: String): List<RemoteCalendar> {
        val res = call(
            HttpMethod.Get,
            "$BASE/users/me/calendarList?maxResults=250" +
                "&fields=items(id,summary,summaryOverride,backgroundColor,primary)",
            token,
            null,
        )
        val items = res["items"]?.jsonArray ?: return emptyList()
        return items.map { el ->
            val cal = el.jsonObject
            RemoteCalendar(
                id = cal.getValue("id").jsonPrimitive.content,
                // 공유 캘린더에 사용자가 붙인 별칭(summaryOverride)이 있으면 우선.
                name = cal.str("summaryOverride").ifBlank { cal.str("summary") }
                    .ifBlank { cal.getValue("id").jsonPrimitive.content },
                colorHex = cal.str("backgroundColor").takeIf { it.isNotBlank() },
                primary = cal["primary"]?.jsonPrimitive?.content == "true",
            )
        }
    }

    /**
     * [calendarId] 캘린더의 [timeMinMillis, timeMaxMillis) 이벤트 목록.
     * 반복 일정은 단일 인스턴스로 펼친다(singleEvents). 한 창(window)당 최대 2500건 — 개인 캘린더엔 충분.
     */
    suspend fun listEvents(token: String, calendarId: String, timeMinMillis: Long, timeMaxMillis: Long): List<RemoteEvent> {
        val encodedId = calendarId.encodeURLParameter()
        val timeMin = Instant.fromEpochMilliseconds(timeMinMillis).toString().encodeURLParameter()
        val timeMax = Instant.fromEpochMilliseconds(timeMaxMillis).toString().encodeURLParameter()
        val res = call(
            HttpMethod.Get,
            "$BASE/calendars/$encodedId/events" +
                "?singleEvents=true&orderBy=startTime&maxResults=2500" +
                "&timeMin=$timeMin&timeMax=$timeMax" +
                "&fields=items(id,status,summary,start,end)",
            token,
            null,
        )
        val items = res["items"]?.jsonArray ?: return emptyList()
        return items.mapNotNull { el ->
            val ev = el.jsonObject
            if (ev.str("status") == "cancelled") return@mapNotNull null
            val start = ev["start"]?.jsonObject ?: return@mapNotNull null
            val end = ev["end"]?.jsonObject
            val startDate = start.str("date").takeIf { it.isNotBlank() }
            if (startDate != null) {
                // 종일 이벤트 — end.date 는 exclusive(다음날) → 포함 종료일은 하루 앞.
                val s = LocalDate.parse(startDate)
                val endExclusive = end?.str("date")?.takeIf { it.isNotBlank() }?.let(LocalDate::parse)
                val lastDay = endExclusive?.minus(1, DateTimeUnit.DAY)?.takeIf { it > s }
                RemoteEvent(
                    id = ev.getValue("id").jsonPrimitive.content,
                    title = ev.str("summary").ifBlank { "(제목 없음)" },
                    startMillis = s.startOfDayMillis(),
                    endMillis = lastDay?.startOfDayMillis(),
                    allDay = true,
                )
            } else {
                val startDateTime = start.str("dateTime").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val sMillis = Instant.parse(startDateTime).toEpochMilliseconds()
                val eMillis = end?.str("dateTime")?.takeIf { it.isNotBlank() }
                    ?.let { Instant.parse(it).toEpochMilliseconds() }
                    ?.takeIf { it > sMillis }
                RemoteEvent(
                    id = ev.getValue("id").jsonPrimitive.content,
                    title = ev.str("summary").ifBlank { "(제목 없음)" },
                    startMillis = sMillis,
                    endMillis = eMillis,
                    allDay = false,
                )
            }
        }
    }

    private fun eventBody(summary: String, description: String, startMillis: Long, allDay: Boolean): String =
        buildJsonObject {
            put("summary", summary.ifBlank { "(제목 없음)" })
            if (description.isNotBlank()) put("description", description)
            if (allDay) {
                val date = startMillis.toLocalDate()
                val end = date.plus(1, DateTimeUnit.DAY)
                putJsonObject("start") { put("date", date.toString()) }
                putJsonObject("end") { put("date", end.toString()) }
            } else {
                val start = Instant.fromEpochMilliseconds(startMillis)
                val end = start.plus(1, DateTimeUnit.HOUR)
                putJsonObject("start") { put("dateTime", start.toString()) } // RFC3339, e.g. 2026-06-30T05:00:00Z
                putJsonObject("end") { put("dateTime", end.toString()) }
            }
        }.toString()

    private suspend fun call(method: HttpMethod, url: String, token: String, body: String?): JsonObject {
        val response = client.request(url) {
            this.method = method
            header("Authorization", "Bearer $token")
            header("Accept", "application/json")
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        val text = response.bodyAsText()
        val code = response.status.value
        if (code !in 200..299) {
            throw CalendarApiException(code, "Calendar API $code: ${text.take(300)}")
        }
        return if (text.isBlank()) JsonObject(emptyMap()) else Json.parseToJsonElement(text).jsonObject
    }

    private fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.content.orEmpty()

    private companion object {
        const val BASE = "https://www.googleapis.com/calendar/v3"
    }
}
