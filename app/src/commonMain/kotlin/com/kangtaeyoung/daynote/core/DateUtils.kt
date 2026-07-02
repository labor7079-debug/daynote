package com.kangtaeyoung.daynote.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * 캘린더 동선용 날짜 유틸. Note.date / Task.dueDate 는 epoch millis 이고, 캘린더는 [LocalDate] 로
 * 다루므로 둘 사이를 변환한다. 시간대는 시스템 기본값.
 */

val appTimeZone: TimeZone get() = TimeZone.currentSystemDefault()

/** 오늘(로컬 날짜). [nowMillis] 의 expect/actual 을 재사용한다. */
fun today(tz: TimeZone = appTimeZone): LocalDate =
    Instant.fromEpochMilliseconds(nowMillis()).toLocalDateTime(tz).date

/** 이 날짜의 자정(시작) epoch millis — 새 메모/할일의 date/dueDate 에 저장할 값. */
fun LocalDate.startOfDayMillis(tz: TimeZone = appTimeZone): Long =
    atStartOfDayIn(tz).toEpochMilliseconds()

/** 하루를 [시작, 다음날 시작) 반열린 구간(epoch millis)으로. DAO 의 범위 조회에 쓴다. */
fun LocalDate.dayRange(tz: TimeZone = appTimeZone): Pair<Long, Long> =
    startOfDayMillis(tz) to plus(1, DateTimeUnit.DAY).startOfDayMillis(tz)

/** epoch millis → LocalDate. */
fun Long.toLocalDate(tz: TimeZone = appTimeZone): LocalDate =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(tz).date

/** 이 날짜의 특정 시각(hour:minute) → epoch millis. (시간 지정 할 일용) */
fun LocalDate.atTimeMillis(hour: Int, minute: Int, tz: TimeZone = appTimeZone): Long =
    LocalDateTime(year, monthNumber, dayOfMonth, hour, minute).toInstant(tz).toEpochMilliseconds()

/** 이 시각(epoch millis)의 '시:분'은 유지한 채 날짜만 [date] 로 옮긴다. (할 일 날짜 이동/복사용) */
fun Long.movedToDate(date: LocalDate, tz: TimeZone = appTimeZone): Long {
    val t = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz)
    return date.atTimeMillis(t.hour, t.minute, tz)
}

/** epoch millis → "HH:mm" 라벨. (시간 지정 할 일 표시용) */
fun Long.toHourMinuteLabel(tz: TimeZone = appTimeZone): String {
    val t = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz)
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
}

/**
 * 이 시각이 자정(=날짜만 지정)인지. Task.endDate 는 날짜만 지정하면 자정 millis,
 * 종료 '시각'까지 지정하면 그 시각 millis 라서 이 검사로 둘을 구분한다.
 */
fun Long.isMidnight(tz: TimeZone = appTimeZone): Boolean = this == toLocalDate(tz).startOfDayMillis(tz)

/** epoch millis 의 시(0-23). (할 일 수정 다이얼로그 초기값용) */
fun Long.hourOfDay(tz: TimeZone = appTimeZone): Int =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(tz).hour

/** epoch millis 의 분(0-59). (할 일 수정 다이얼로그 초기값용) */
fun Long.minuteOfHour(tz: TimeZone = appTimeZone): Int =
    Instant.fromEpochMilliseconds(this).toLocalDateTime(tz).minute

/**
 * ISO 8601 연중 주차(W1~W53) — "그 주의 목요일이 속한 해의 몇 번째 주"로 정의된다.
 * (연초·연말 경계 주도 표준대로: 예. 2027-01-01(금)은 2026년 W53.)
 */
fun LocalDate.isoWeekNumber(): Int {
    val thursday = plus(4 - dayOfWeek.isoDayNumber, DateTimeUnit.DAY)
    return (thursday.dayOfYear - 1) / 7 + 1
}

/** 이 날짜가 속한 주의 시작(월요일). */
fun LocalDate.startOfWeek(): LocalDate {
    val shift = dayOfWeek.isoDayNumber - 1 // 월=1 → 0
    return minus(shift, DateTimeUnit.DAY)
}

/** 월요일 시작 7일. */
fun LocalDate.weekDays(): List<LocalDate> {
    val start = startOfWeek()
    return (0 until 7).map { start.plus(it, DateTimeUnit.DAY) }
}

/** 이 달 달력 그리드(월요일 시작, 6주=42칸)의 날짜들. 앞뒤 달 날짜를 채워 칸을 맞춘다. */
fun monthGridDays(anchor: LocalDate): List<LocalDate> {
    val firstOfMonth = LocalDate(anchor.year, anchor.month, 1)
    val gridStart = firstOfMonth.startOfWeek()
    return (0 until 42).map { gridStart.plus(it, DateTimeUnit.DAY) }
}

/** 그리드/주 범위(첫날 시작 ~ 마지막날 다음날 시작)를 epoch millis 로. 범위 조회용. */
fun List<LocalDate>.toMillisRange(tz: TimeZone = appTimeZone): Pair<Long, Long> {
    val start = first().startOfDayMillis(tz)
    val end = last().plus(1, DateTimeUnit.DAY).startOfDayMillis(tz)
    return start to end
}

/** 달 이동(첫날 기준 ±N개월). */
fun LocalDate.firstOfMonthPlusMonths(months: Int): LocalDate =
    LocalDate(year, month, 1).plus(months, DateTimeUnit.MONTH)
