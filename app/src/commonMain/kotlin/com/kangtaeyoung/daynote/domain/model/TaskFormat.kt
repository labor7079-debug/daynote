package com.kangtaeyoung.daynote.domain.model

import com.kangtaeyoung.daynote.core.isMidnight
import com.kangtaeyoung.daynote.core.toHourMinuteLabel
import com.kangtaeyoung.daynote.core.toLocalDate

/**
 * 할 일의 일정 라벨(시각/기간 표기) — 캘린더·상세·홈 위젯이 공용으로 쓰는 단일 규칙.
 *
 * - 같은 날 시각 범위: "14:00~16:00"
 * - 여러 날 기간: "7/2~7/4" (시각 지정이면 "7/2 14:00~7/4 16:00" 처럼 시작·종료 시각 병기)
 * - 시각만 지정(종료 없음): "14:00"
 * - 종일 하루짜리: null (표기 없음)
 */
fun Task.scheduleLabel(): String? {
    val due = dueDate ?: return null
    val end = endDate
    return when {
        end != null && end.toLocalDate() == due.toLocalDate() ->
            "${due.toHourMinuteLabel()}~${end.toHourMinuteLabel()}"
        end != null -> {
            val s = due.toLocalDate()
            val e = end.toLocalDate()
            val startLabel =
                if (allDay) "${s.monthNumber}/${s.dayOfMonth}"
                else "${s.monthNumber}/${s.dayOfMonth} ${due.toHourMinuteLabel()}"
            val endLabel =
                if (end.isMidnight()) "${e.monthNumber}/${e.dayOfMonth}"
                else "${e.monthNumber}/${e.dayOfMonth} ${end.toHourMinuteLabel()}"
            "$startLabel~$endLabel"
        }
        !allDay -> due.toHourMinuteLabel()
        else -> null
    }
}
