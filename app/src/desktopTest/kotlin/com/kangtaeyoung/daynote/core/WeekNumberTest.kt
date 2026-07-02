package com.kangtaeyoung.daynote.core

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** ISO 8601 연중 주차 계산 검증 — 연초·연말 경계 포함. */
class WeekNumberTest {

    @Test
    fun isoWeekNumberMatchesKnownDates() {
        // 2026-01-01(목) → 2026년 W1 (ISO: 목요일이 속한 해의 주).
        assertEquals(1, LocalDate(2026, 1, 1).isoWeekNumber())
        // 2026-06-29(월) 주 = W27 (W1 이 2025-12-29 시작이므로 26주 뒤).
        assertEquals(27, LocalDate(2026, 6, 29).isoWeekNumber())
        assertEquals(27, LocalDate(2026, 7, 2).isoWeekNumber())
        // 2027-01-01(금)은 2026년 W53 에 속한다(경계 주).
        assertEquals(53, LocalDate(2027, 1, 1).isoWeekNumber())
    }
}
