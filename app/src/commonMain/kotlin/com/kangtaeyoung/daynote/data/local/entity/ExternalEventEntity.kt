package com.kangtaeyoung.daynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 구글 캘린더에서 읽어온(pull) 외부 일정 캐시 — **읽기 전용**.
 *
 * 설정에서 체크한 캘린더(공유받은 캘린더 포함)의 일정을 동기화 때 통째로 교체 저장하고,
 * 달력 화면은 이 테이블만 관찰한다(오프라인에서도 마지막 pull 결과가 보인다).
 * 앱의 메모/할 일(진실의 원천)과 섞이지 않으며, 수정·삭제 UI 를 제공하지 않는다.
 */
@Entity(
    tableName = "external_events",
    indices = [Index("startMillis"), Index("calendarId")],
)
data class ExternalEventEntity(
    @PrimaryKey val id: String, // "calendarId:eventId" — 캘린더 간 이벤트 id 충돌 방지
    val calendarId: String,
    val calendarName: String,
    val title: String,
    val startMillis: Long,
    /** 종료(포함, epoch millis). 하루짜리 종일 일정은 null. 여러 날 종일 일정은 마지막 날 자정. */
    val endMillis: Long? = null,
    val allDay: Boolean = true,
    /** 캘린더 배경색("#RRGGBB") — 구글 캘린더의 캘린더별 색 구분을 그대로 따른다. */
    val colorHex: String? = null,
)
