package com.kangtaeyoung.daynote.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * 메모 DAO. 모든 조회는 소프트 삭제([NoteEntity.deletedAt])된 항목을 제외한다.
 *
 * UI 관찰용 쿼리는 `Flow` 로 반환해 Room 이 변경을 자동 방출하도록 한다(MVVM).
 * `updatedAt`/`syncStatus` 등 메타 갱신 규칙은 Repository(Step 3)에서 채운다 — DAO 는 저장/조회만.
 */
@Dao
interface NoteDao {

    // --- 쓰기 ---

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Upsert
    suspend fun upsertAll(notes: List<NoteEntity>)

    /** 소프트 삭제: 실제 행을 지우지 않고 [deletedAt] 만 찍어 동기화가 깨지지 않게 한다. */
    @Query("UPDATE notes SET deletedAt = :timestamp, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long)

    /** 소프트 삭제 복구. */
    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: String, timestamp: Long)

    /** 영구 삭제(테스트·향후 tombstone 정리용). 평상시엔 [softDelete] 를 쓴다. */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDelete(id: String)

    // --- 단건 조회 ---

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): NoteEntity?

    // --- 목록 조회 ---

    /** 활성 메모 전체. 고정(핀) 우선, 그다음 최근 수정 순. */
    @Query(
        """
        SELECT * FROM notes
        WHERE deletedAt IS NULL
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeAll(): Flow<List<NoteEntity>>

    /**
     * 특정 날짜(캘린더 동선)에 속한 메모.
     * [date] 는 epoch millis 라 하루를 [startOfDay, endOfDay) 반열린 구간으로 조회한다.
     */
    @Query(
        """
        SELECT * FROM notes
        WHERE date >= :startOfDay AND date < :endOfDay AND deletedAt IS NULL
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<NoteEntity>>

    /** 날짜 없는 메모(보조 목록). */
    @Query(
        """
        SELECT * FROM notes
        WHERE date IS NULL AND deletedAt IS NULL
        ORDER BY isPinned DESC, updatedAt DESC
        """,
    )
    fun observeUndated(): Flow<List<NoteEntity>>

    // --- 전문 검색 (FTS4) ---

    /**
     * 본문/제목 전문 검색. external-content FTS 테이블([com.kangtaeyoung.daynote.data.local.entity.NoteFtsEntity])과
     * rowid 로 조인한다. [query] 는 FTS MATCH 문법(예: `"키워드*"` 접두 검색)을 따른다 — Repository 에서 정제해 넘긴다.
     */
    @Query(
        """
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.rowid = notes_fts.rowid
        WHERE notes_fts MATCH :query AND notes.deletedAt IS NULL
        ORDER BY notes.updatedAt DESC
        """,
    )
    fun search(query: String): Flow<List<NoteEntity>>
}
