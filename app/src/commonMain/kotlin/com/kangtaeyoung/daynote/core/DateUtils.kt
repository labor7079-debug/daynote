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

/** epoch millis → "HH:mm" 라벨. (시간 지정 할 일 표시용) */
fun Long.toHourMinuteLabel(tz: TimeZone = appTimeZone): String {
    val t = Instant.fromEpochMilliseconds(this).toLocalDateTime(tz)
    return "${t.hour.toString().padStart(2, '0')}:${t.minute.toString().padStart(2, '0')}"
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
