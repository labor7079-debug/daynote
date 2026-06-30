package com.kangtaeyoung.daynote.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 메모 한 건. 진실의 원천(Source of Truth)인 Room 로컬 DB의 핵심 엔티티.
 *
 * - [date] 는 캘린더 연결용 키. null 이면 날짜 없는 메모(보조 목록에서 관리).
 * - 동기화 메타([remoteId]/[syncStatus]/[deletedAt])는 Phase 6 대비로 처음부터 포함해
 *   나중에 스키마 마이그레이션을 피한다.
 * - 삭제는 즉시 삭제가 아니라 [deletedAt] 소프트 삭제로 처리한다.
 */
@Entity(
    tableName = "notes",
    indices = [Index("date"), Index("deletedAt")], // 날짜별 조회 · 소프트삭제 필터
)
data class NoteEntity(
    @PrimaryKey val id: String, // UUID (동기화 대비 클라이언트 생성)
    val title: String,
    val content: String, // 마크다운 원문
    val date: Long? = null, // 캘린더 연결용 (epoch millis, nullable)
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    // --- 동기화 메타 (Phase 6 대비, 미리 포함) ---
    val remoteId: String? = null,
    val syncStatus: String = SyncStatus.LOCAL_ONLY,
    val deletedAt: Long? = null, // 소프트 삭제
)
