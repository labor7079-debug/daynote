package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.today
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus

/**
 * 기간 필터 + 일자별 그룹 헤더 — 메모·To-Do 목록이 공유하는 "언제 것인지" 조회 도구.
 */
enum class Period(val label: String) {
    ALL("전체"),
    TODAY("오늘"),
    WEEK("최근 7일"),
    MONTH("최근 30일"),
}

/** 이 기간의 시작(자정, epoch millis). ALL 은 컷오프 없음(null). */
fun Period.cutoffMillis(): Long? = when (this) {
    Period.ALL -> null
    Period.TODAY -> today().startOfDayMillis()
    Period.WEEK -> today().minus(6, DateTimeUnit.DAY).startOfDayMillis()
    Period.MONTH -> today().minus(29, DateTimeUnit.DAY).startOfDayMillis()
}

/** 기간 선택 칩 한 줄(전체/오늘/최근 7일/최근 30일). */
@Composable
fun PeriodFilterRow(
    selected: Period,
    onSelect: (Period) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Period.entries.forEach { p ->
            FilterChip(
                selected = selected == p,
                onClick = { onSelect(p) },
                label = { Text(p.label) },
            )
        }
    }
}

private val dowLabels = listOf("월", "화", "수", "목", "금", "토", "일")
private fun koreanDow(dow: DayOfWeek): String = dowLabels[dow.isoDayNumber - 1]

/** 일자별 그룹 헤더 — "2026년 7월 2일 (수) · N개". 캘린더 상세와 같은 클리니컬 캡션 톤. */
@Composable
fun DateGroupHeader(date: LocalDate, count: Int, modifier: Modifier = Modifier) {
    Text(
        text = "${date.year}년 ${date.monthNumber}월 ${date.dayOfMonth}일 (${koreanDow(date.dayOfWeek)}) · ${count}개",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}
