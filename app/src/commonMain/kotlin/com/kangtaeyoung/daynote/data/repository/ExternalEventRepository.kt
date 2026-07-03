package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.data.local.dao.ExternalEventDao
import com.kangtaeyoung.daynote.data.local.entity.ExternalEventEntity
import com.kangtaeyoung.daynote.domain.model.ExternalEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 구글 캘린더 외부 일정(읽기 전용) 조회 — UI 는 데이터 출처를 모른다(설계원칙 4). */
interface ExternalEventRepository {
    /** [start, endExclusive) 와 겹치는 외부 일정. */
    fun observeByDateRange(start: Long, endExclusive: Long): Flow<List<ExternalEvent>>
}

class ExternalEventRepositoryImpl(
    private val dao: ExternalEventDao,
) : ExternalEventRepository {

    override fun observeByDateRange(start: Long, endExclusive: Long): Flow<List<ExternalEvent>> =
        dao.observeByRange(start, endExclusive).map { list -> list.map { it.toDomain() } }
}

private fun ExternalEventEntity.toDomain() = ExternalEvent(
    id = id,
    calendarId = calendarId,
    calendarName = calendarName,
    title = title,
    startMillis = startMillis,
    endMillis = endMillis,
    allDay = allDay,
    colorHex = colorHex,
)
