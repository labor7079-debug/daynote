package com.kangtaeyoung.daynote.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kangtaeyoung.daynote.data.local.entity.AiResultEntity
import kotlinx.coroutines.flow.Flow

/** AI 결과 접근. 메모별 최신순 조회 + 저장. */
@Dao
interface AiResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: AiResultEntity)

    @Query("SELECT * FROM ai_results WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun observeForNote(noteId: String): Flow<List<AiResultEntity>>

    /** 이력 항목 1건 삭제(사용자가 지운 결과). 스키마 변경 없음 — 행 삭제뿐. */
    @Query("DELETE FROM ai_results WHERE id = :id")
    suspend fun deleteById(id: String)
}
