package com.kangtaeyoung.daynote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kangtaeyoung.daynote.data.local.entity.ExternalEventEntity
import kotlinx.coroutines.flow.Flow

/** 구글 캘린더 외부 일정 캐시 접근 — 동기화(pull)가 쓰고, 달력 화면이 관찰한다. */
@Dao
interface ExternalEventDao {

    /** 범위와 겹치는 일정(여러 날 일정 포함). 종일 먼저, 시작 시각 순. */
    @Query(
        "SELECT * FROM external_events " +
            "WHERE startMillis < :endExclusive AND COALESCE(endMillis, startMillis) >= :start " +
            "ORDER BY allDay DESC, startMillis ASC",
    )
    fun observeByRange(start: Long, endExclusive: Long): Flow<List<ExternalEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<ExternalEventEntity>)

    @Query("DELETE FROM external_events WHERE calendarId = :calendarId")
    suspend fun deleteByCalendar(calendarId: String)

    @Query("DELETE FROM external_events WHERE calendarId NOT IN (:keep)")
    suspend fun deleteExceptCalendars(keep: List<String>)

    @Query("DELETE FROM external_events")
    suspend fun deleteAll()

    /** 한 캘린더의 일정을 통째로 교체(pull 결과 반영). */
    @Transaction
    suspend fun replaceForCalendar(calendarId: String, events: List<ExternalEventEntity>) {
        deleteByCalendar(calendarId)
        if (events.isNotEmpty()) insertAll(events)
    }
}
