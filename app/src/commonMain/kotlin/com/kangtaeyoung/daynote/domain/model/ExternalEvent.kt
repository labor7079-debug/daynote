package com.kangtaeyoung.daynote.domain.model

/** 구글 캘린더에서 읽어온 외부 일정(읽기 전용) — 달력 표시용 도메인 모델. */
data class ExternalEvent(
    val id: String,
    val calendarId: String,
    val calendarName: String,
    val title: String,
    val startMillis: Long,
    /** 종료(포함). 하루짜리 종일 일정은 null. */
    val endMillis: Long?,
    val allDay: Boolean,
    /** 캘린더 배경색("#RRGGBB") — 없으면 기본 톤으로 그린다. */
    val colorHex: String?,
)
