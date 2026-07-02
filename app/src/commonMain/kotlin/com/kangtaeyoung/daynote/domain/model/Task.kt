package com.kangtaeyoung.daynote.domain.model

/**
 * 할 일 도메인 모델 — 체크리스트형 To-Do.
 *
 * [noteId] 가 있으면 특정 메모 소속, null 이면 독립 할 일.
 * 동기화 메타는 [Note] 와 같은 이유로 노출하지 않는다.
 */
data class Task(
    val id: String,
    val noteId: String? = null,
    val text: String,
    val isDone: Boolean = false,
    val dueDate: Long? = null, // 캘린더 연결용 (epoch millis, nullable)
    val allDay: Boolean = true, // dueDate 가 종일인지 특정 시각인지
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    /** 기간 할 일의 종료 날짜(자정 millis). null 이면 하루짜리. 캘린더에서 bar 로 걸쳐 표시. */
    val endDate: Long? = null,
)
