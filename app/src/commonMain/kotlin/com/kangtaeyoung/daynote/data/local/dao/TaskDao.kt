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

    /** 백업용 — 소프트 삭제 포함 모든 행(충실한 내보내기). */
    @Query("SELECT * FROM tasks")
    suspend fun getAllRaw(): List<TaskEntity>

    /** 알림 예약용 — 앞으로 마감인 '시간 지정'(allDay=0) 활성·미완료 할 일. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND isDone = 0 AND allDay = 0
          AND dueDate IS NOT NULL AND dueDate > :now
        """,
    )
    suspend fun getTimedUpcoming(now: Long): List<TaskEntity>

    /** 알림 예약용 — 오늘 이후 마감인 '종일'(allDay=1) 활성·미완료 할 일(기본시각 알림). */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND isDone = 0 AND allDay = 1
          AND dueDate IS NOT NULL AND dueDate >= :startOfToday
        """,
    )
    suspend fun getAllDayUpcoming(startOfToday: Long): List<TaskEntity>

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
     * 특정 날짜(캘린더 동선)의 할 일 — **기간 겹침** 조회. 하루짜리(endDate=null)는 dueDate 가
     * 구간에 들면, 기간 할 일은 [dueDate, endDate] 가 구간과 겹치면 잡힌다(캘린더 bar 표시).
     * 미완료 → 완료, 그다음 정렬순.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate IS NOT NULL AND deletedAt IS NULL
          AND dueDate < :endOfDay AND COALESCE(endDate, dueDate) >= :startOfDay
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

    /**
     * 메모에 속하지 않은(독립) 모든 할 일 — 마감일 유무와 무관.
     * To-Do 탭의 마스터 목록(캘린더에서 날짜를 지정해 만든 할 일도 여기 포함된다).
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE noteId IS NULL AND deletedAt IS NULL
        ORDER BY isDone ASC, dueDate IS NULL, dueDate ASC, sortOrder ASC, createdAt ASC
        """,
    )
    fun observeStandalone(): Flow<List<TaskEntity>>

    // --- 동기화(Phase 3) ---

    /** 캘린더로 내보낼 할 일: 마감일 있음 · 활성 · 아직 SYNCED 아님. */
    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND deletedAt IS NULL AND syncStatus != 'SYNCED'")
    suspend fun getTasksToPush(): List<TaskEntity>

    /** 원격 이벤트를 지워야 할 할 일: 소프트 삭제됐고 remoteId 가 남아 있음. */
    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL AND remoteId IS NOT NULL")
    suspend fun getDeletedTasksWithRemote(): List<TaskEntity>

    @Query("UPDATE tasks SET remoteId = :remoteId, syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String, remoteId: String)

    @Query("UPDATE tasks SET remoteId = NULL, syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun clearRemote(id: String)

    // --- 클라우드 동기화(Phase 6, Supabase · 워터마크 델타) ---

    /** [since] 이후 변경된 모든 할 일(삭제 tombstone 포함). 클라우드 push 대상. */
    @Query("SELECT * FROM tasks WHERE updatedAt > :since")
    suspend fun getTasksModifiedSince(since: Long): List<TaskEntity>

    /** id 로 원본 행 조회(소프트 삭제 포함). pull 충돌 비교용. */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getByIdRaw(id: String): TaskEntity?
}
