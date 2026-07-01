package com.kangtaeyoung.daynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AI 호출 결과 저장(진실의 원천). noteId 가 있으면 그 메모에 속한 결과로 묶어 조회한다.
 * action 은 [com.kangtaeyoung.daynote.domain.model.AiAction] 의 name(String) 으로 보관.
 */
@Entity(tableName = "ai_results", indices = [Index("noteId"), Index("createdAt")])
data class AiResultEntity(
    @PrimaryKey val id: String,
    val noteId: String?,
    val action: String,
    val sourceText: String,
    val resultText: String,
    val model: String,
    val createdAt: Long,
)
