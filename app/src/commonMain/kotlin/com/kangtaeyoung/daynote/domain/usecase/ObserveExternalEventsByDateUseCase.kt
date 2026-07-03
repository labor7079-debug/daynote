package com.kangtaeyoung.daynote.domain.usecase

import com.kangtaeyoung.daynote.data.repository.ExternalEventRepository
import com.kangtaeyoung.daynote.domain.model.ExternalEvent
import kotlinx.coroutines.flow.Flow

/** 날짜 범위와 겹치는 구글 캘린더 외부 일정(읽기 전용) 관찰 — 달력 칸·상세 표시용. */
class ObserveExternalEventsByDateUseCase(
    private val repository: ExternalEventRepository,
) {
    operator fun invoke(start: Long, endExclusive: Long): Flow<List<ExternalEvent>> =
        repository.observeByDateRange(start, endExclusive)
}
