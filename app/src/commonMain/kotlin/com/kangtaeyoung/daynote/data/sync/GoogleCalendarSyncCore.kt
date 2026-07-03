package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.data.local.dao.ExternalEventDao
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.local.entity.ExternalEventEntity
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * 구글 캘린더 동기화 본체 — **플랫폼 공용**(commonMain). 액세스 토큰만 받으면
 * push(앱→구글)와 공유 캘린더 pull 을 수행한다. 토큰 확보(인증)는 플랫폼 매니저
 * (Android=Play 서비스 무음 인가, Desktop=PKCE 루프백)가 담당한다.
 */
class GoogleCalendarSyncCore(
    private val api: GoogleCalendarApi,
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val settings: SettingsRepository,
    private val externalEventDao: ExternalEventDao,
) {

    /** 한 번의 동기화 — 앱→구글 push 후, 체크된 캘린더의 일정 pull(외부 캐시 교체). */
    suspend fun sync(token: String) {
        pushAll(token)
        pullExternal(token)
    }

    suspend fun listCalendars(token: String): List<GoogleCalendarInfo> =
        api.listCalendars(token).map { GoogleCalendarInfo(id = it.id, name = it.name, colorHex = it.colorHex, primary = it.primary) }

    /** 실제 push 본체 — 삭제 반영 → 메모 → 할 일. 실패는 예외로 올려 호출자가 처리한다. */
    private suspend fun pushAll(token: String) {
        // 1) 삭제 반영: 소프트 삭제됐고 원격 이벤트가 있는 메모/할 일 → 이벤트 삭제.
        for (note in noteDao.getDeletedNotesWithRemote()) {
            val remoteId = note.remoteId ?: continue
            api.deleteEvent(token, remoteId)
            noteDao.clearRemote(note.id)
        }
        for (task in taskDao.getDeletedTasksWithRemote()) {
            val remoteId = task.remoteId ?: continue
            api.deleteEvent(token, remoteId)
            taskDao.clearRemote(task.id)
        }
        // 2) 신규/수정 push: 날짜 있는 활성 메모 → 종일 이벤트.
        for (note in noteDao.getNotesToPush()) {
            val start = note.date ?: continue
            val remoteId = note.remoteId
            if (remoteId == null) {
                val eventId = api.insertEvent(token, note.title, note.content, start, allDay = true)
                noteDao.markSynced(note.id, eventId)
            } else {
                api.updateEvent(token, remoteId, note.title, note.content, start, allDay = true)
                noteDao.markSynced(note.id, remoteId)
            }
        }
        // 3) 마감일 있는 할 일 → 종일/시간 이벤트(allDay 플래그).
        for (task in taskDao.getTasksToPush()) {
            val start = task.dueDate ?: continue
            val remoteId = task.remoteId
            if (remoteId == null) {
                val eventId = api.insertEvent(token, task.text, "", start, allDay = task.allDay)
                taskDao.markSynced(task.id, eventId)
            } else {
                api.updateEvent(token, remoteId, task.text, "", start, allDay = task.allDay)
                taskDao.markSynced(task.id, remoteId)
            }
        }
    }

    /**
     * 설정에서 체크한 캘린더(공유받은 것 포함)의 일정을 오늘 기준 -60일~+180일 창으로 읽어
     * 로컬 캐시를 캘린더 단위로 통째로 교체한다. 체크 해제된 캘린더의 캐시는 지운다.
     * 아무것도 체크돼 있지 않으면 API 호출 없이 캐시만 비운다(범위 미동의 계정도 기본 동기화는 무사).
     */
    private suspend fun pullExternal(token: String) {
        val visible = settings.getVisibleGoogleCalendarIds()
        if (visible.isEmpty()) {
            externalEventDao.deleteAll()
            return
        }
        externalEventDao.deleteExceptCalendars(visible.toList())

        val windowStart = today().plus(-PULL_PAST_DAYS, DateTimeUnit.DAY).startOfDayMillis()
        val windowEnd = today().plus(PULL_FUTURE_DAYS, DateTimeUnit.DAY).startOfDayMillis()
        // 이름·색은 calendarList 에서 — 실패해도 일정 pull 은 계속(이름은 id 로 대체).
        val calInfo = runCatching { api.listCalendars(token) }.getOrNull().orEmpty().associateBy { it.id }

        for (calendarId in visible) {
            val events = api.listEvents(token, calendarId, windowStart, windowEnd)
            val info = calInfo[calendarId]
            externalEventDao.replaceForCalendar(
                calendarId,
                events.map { ev ->
                    ExternalEventEntity(
                        id = "$calendarId:${ev.id}",
                        calendarId = calendarId,
                        calendarName = info?.name ?: calendarId,
                        title = ev.title,
                        startMillis = ev.startMillis,
                        endMillis = ev.endMillis,
                        allDay = ev.allDay,
                        colorHex = info?.colorHex,
                    )
                },
            )
        }
    }

    private companion object {
        const val PULL_PAST_DAYS = 60
        const val PULL_FUTURE_DAYS = 180
    }
}
