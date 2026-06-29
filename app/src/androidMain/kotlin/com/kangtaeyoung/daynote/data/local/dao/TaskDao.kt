package com.kangtaeyoung.daynote.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * 할 일 DAO. 메모와 동일하게 소프트 삭제([TaskEntity.deletedAt]) 항목은 조회에서 제외한다.
 */
@Dao
interface TaskDao {

    // --- 쓰기 ---

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    /** 완료/미완료 토글. UI 의 체크박스 동선에 맞춰 단일 UPDATE 로 원자적으로 뒤집는다. */
    @Query("UPDATE tasks SET isDone = NOT isDone, updatedAt = :timestamp WHERE id = :id")
    suspend fun toggleDone(id: String, timestamp: Long)

    @Query("UPDATE tasks SET isDone = :isDone, updatedAt = :timestamp WHERE id = :id")
    suspend fun setDone(id: String, isDone: Boolean, timestamp: Long)

    @Query("UPDATE tasks SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    @Query("UPDATE tasks SET deletedAt = NULL, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: String, timestamp: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun hardDelete(id: String)

    // --- 단건 조회 ---

    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): TaskEntity?

    // --- 목록 조회 ---

    /** 특정 메모에 속한 할 일. 정렬순 → 생성순. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE noteId = :noteId AND deletedAt IS NULL
        ORDER BY sortOrder ASC, createdAt ASC
        """,
    )
    fun observeByNote(noteId: String): Flow<List<TaskEntity>>

    /**
     * 특정 날짜(캘린더 동선)의 할 일. [dueDate] 를 [startOfDay, endOfDay) 로 조회한다.
     * 미완료 → 완료, 그다음 정렬순.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate >= :startOfDay AND dueDate < :endOfDay AND deletedAt IS NULL
        ORDER BY isDone ASC, sortOrder ASC, createdAt ASC
        """,
    )
    fun observeByDueDateRange(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    /** 마감일 없는 할 일(보조 목록). */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate IS NULL AND deletedAt IS NULL
        ORDER BY isDone ASC, sortOrder ASC, createdAt ASC
        """,
    )
    fun observeUndated(): Flow<List<TaskEntity>>
}
