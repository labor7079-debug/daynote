package com.kangtaeyoung.daynote.domain.holiday

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 공휴일 테이블 검증 — 월력요항(우주항공청 발표) 대표값과 제도 변경(제헌절 재지정 등) 반영 확인. */
class KoreanHolidaysTest {

    @Test
    fun knownHolidaysAreMarked() {
        // 2025 — 추석 연휴 일요일 겹침 → 대체공휴일 10/8
        assertEquals("추석", KoreanHolidays.nameOf(LocalDate(2025, 10, 6)))
        assertEquals("대체공휴일(추석)", KoreanHolidays.nameOf(LocalDate(2025, 10, 8)))
        // 2026 — 제헌절 재지정(법률 제21338호, 2026-05-11 시행)
        assertEquals("제헌절", KoreanHolidays.nameOf(LocalDate(2026, 7, 17)))
        assertEquals("대체공휴일(개천절)", KoreanHolidays.nameOf(LocalDate(2026, 10, 5)))
        // 2027 — 월력요항(2026-06-29 발표): 설날 일요일 → 대체 2/9, 노동절 첫 반영
        assertEquals("대체공휴일(설날)", KoreanHolidays.nameOf(LocalDate(2027, 2, 9)))
        assertEquals("노동절", KoreanHolidays.nameOf(LocalDate(2027, 5, 1)))
        assertTrue(KoreanHolidays.isHoliday(LocalDate(2027, 9, 15))) // 추석
    }

    @Test
    fun ordinaryWeekdaysAreNotHolidays() {
        assertFalse(KoreanHolidays.isHoliday(LocalDate(2026, 7, 2)))  // 평일(목)
        assertFalse(KoreanHolidays.isHoliday(LocalDate(2027, 6, 7)))  // 현충일 다음날 — 대체공휴일 아님
    }
}
