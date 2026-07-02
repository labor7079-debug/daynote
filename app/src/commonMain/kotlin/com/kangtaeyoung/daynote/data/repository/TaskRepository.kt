package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * 할 일 데이터 접근 추상화. 메모와 동일한 정책(Flow 읽기 · suspend 쓰기 · 소프트삭제).
 */
interface TaskRepository {

    /** 특정 메모에 속한 할 일. */
    fun observeTasksForNote(noteId: String): Flow<List<Task>>

    /** 특정 날짜 구간 [startOfDay, endOfDay) 의 할 일 (캘린더 동선, Phase 2). */
    fun observeTasksByDueDate(startOfDay: Long, endOfDay: Long): Flow<List<Task>>

    /** 메모에 속하지 않은(독립) 할 일 목록(To-Do 탭) — 마감일 유무 무관. */
    fun observeGeneralTasks(): Flow<List<Task>>

    suspend fun getTask(id: String): Task?

    /**
     * 새 할 일 생성 — id·타임스탬프를 구현체가 채운다. [allDay]=false 면 [dueDate] 는 특정 시각.
     * [endDate] 가 있으면 여러 날에 걸친 기간 할 일(캘린더에 bar 로 표시).
     */
    suspend fun addTask(
        text: String,
        noteId: String? = null,
        dueDate: Long? = null,
        allDay: Boolean = true,
        endDate: Long? = null,
        sortOrder: Int = 0,
    ): Task

    suspend fun updateTask(task: Task)

    /** 완료/미완료 토글(원자적). */
    suspend fun toggleDone(id: String)

    suspend fun deleteTask(id: String)

    suspend fun restoreTask(id: String)
}
