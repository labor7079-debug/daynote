package com.kangtaeyoung.daynote.domain.usecase

import com.kangtaeyoung.daynote.data.repository.TaskRepository
import com.kangtaeyoung.daynote.domain.model.Task
import kotlinx.coroutines.flow.Flow

/**
 * 할 일 UseCase 모음. 메모와 같은 방식의 얇은 래퍼.
 */

class ObserveNoteTasksUseCase(private val repo: TaskRepository) {
    operator fun invoke(noteId: String): Flow<List<Task>> = repo.observeTasksForNote(noteId)
}

class ObserveGeneralTasksUseCase(private val repo: TaskRepository) {
    operator fun invoke(): Flow<List<Task>> = repo.observeGeneralTasks()
}

/** 특정 날짜 구간 [startOfDay, endOfDay) 의 할 일 (캘린더 동선). */
class ObserveTasksByDateUseCase(private val repo: TaskRepository) {
    operator fun invoke(startOfDay: Long, endOfDay: Long): Flow<List<Task>> =
        repo.observeTasksByDueDate(startOfDay, endOfDay)
}

/** 새 할 일 추가. 빈 텍스트는 무시(null). [allDay]=false 면 [dueDate] 는 특정 시각. */
class AddTaskUseCase(private val repo: TaskRepository) {
    suspend operator fun invoke(
        text: String,
        noteId: String? = null,
        dueDate: Long? = null,
        allDay: Boolean = true,
    ): Task? {
        if (text.isBlank()) return null
        return repo.addTask(text.trim(), noteId, dueDate, allDay)
    }
}

/** 기존 할 일 갱신(텍스트·마감일·정렬 등) — updatedAt 은 구현체가 찍는다. */
class UpdateTaskUseCase(private val repo: TaskRepository) {
    suspend operator fun invoke(task: Task) = repo.updateTask(task)
}

class ToggleTaskUseCase(private val repo: TaskRepository) {
    suspend operator fun invoke(id: String) = repo.toggleDone(id)
}

class DeleteTaskUseCase(private val repo: TaskRepository) {
    suspend operator fun invoke(id: String) = repo.deleteTask(id)
}
