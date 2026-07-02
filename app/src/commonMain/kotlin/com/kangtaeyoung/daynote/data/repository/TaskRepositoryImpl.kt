package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.core.randomUuid
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.local.entity.SyncStatus
import com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier
import com.kangtaeyoung.daynote.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [TaskRepository] 의 로컬(Room) 구현. id·타임스탬프 규칙을 채우고 엔티티↔도메인 매핑을 담당한다.
 */
class TaskRepositoryImpl(
    private val dao: TaskDao,
    private val changes: LocalChangeNotifier,
) : TaskRepository {

    override fun observeTasksForNote(noteId: String): Flow<List<Task>> =
        dao.observeByNote(noteId).map { list -> list.map { it.toDomain() } }

    override fun observeTasksByDueDate(startOfDay: Long, endOfDay: Long): Flow<List<Task>> =
        dao.observeByDueDateRange(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }

    override fun observeGeneralTasks(): Flow<List<Task>> =
        dao.observeStandalone().map { list -> list.map { it.toDomain() } }

    override suspend fun getTask(id: String): Task? = dao.getById(id)?.toDomain()

    override suspend fun addTask(
        text: String,
        noteId: String?,
        dueDate: Long?,
        allDay: Boolean,
        endDate: Long?,
        sortOrder: Int,
    ): Task {
        val now = nowMillis()
        val task = Task(
            id = randomUuid(),
            noteId = noteId,
            text = text,
            isDone = false,
            dueDate = dueDate,
            allDay = allDay,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now,
            // 종료일은 시작일 이후일 때만 의미가 있다(같거나 이전이면 하루짜리로 정규화).
            endDate = endDate?.takeIf { dueDate != null && it > dueDate },
        )
        dao.upsert(task.toEntity())
        changes.notifyChanged()
        return task
    }

    override suspend fun updateTask(task: Task) {
        // 동기화 메타(remoteId 등) 보존 + 변경을 PENDING 으로 표시(메모와 동일 정책).
        val existing = dao.getById(task.id) ?: return
        dao.upsert(
            existing.copy(
                text = task.text,
                isDone = task.isDone,
                dueDate = task.dueDate,
                allDay = task.allDay,
                endDate = task.endDate?.takeIf { task.dueDate != null && it > task.dueDate!! },
                sortOrder = task.sortOrder,
                updatedAt = nowMillis(),
                syncStatus = SyncStatus.PENDING,
            ),
        )
        changes.notifyChanged()
    }

    override suspend fun toggleDone(id: String) {
        dao.toggleDone(id, nowMillis())
        changes.notifyChanged()
    }

    override suspend fun deleteTask(id: String) {
        dao.softDelete(id, nowMillis())
        changes.notifyChanged()
    }

    override suspend fun restoreTask(id: String) {
        dao.restore(id, nowMillis())
        changes.notifyChanged()
    }
}
