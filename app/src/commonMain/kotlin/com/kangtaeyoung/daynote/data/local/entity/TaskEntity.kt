package com.kangtaeyoung.daynote.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 할 일 한 건. 체크리스트형 To-Do.
 *
 * - [noteId] 가 있으면 특정 메모 소속, null 이면 독립 할 일.
 * - [dueDate] 는 캘린더 연결용 마감일 키 (nullable).
 * - 동기화 메타와 소프트 삭제는 [NoteEntity] 와 동일한 정책을 따른다.
 */
@Entity(
    tableName = "tasks",
    indices = [Index("dueDate"), Index("noteId"), Index("deletedAt")],
)
data class TaskEntity(
    @PrimaryKey val id: String, // UUID
    val noteId: String? = null, // 특정 메모 소속이면 FK, 독립이면 null
    val text: String,
    val isDone: Boolean = false,
    val dueDate: Long? = null, // 캘린더 연결용 (epoch millis, nullable)
    // 마이그레이션(ALTER TABLE … DEFAULT 1)과 신규 생성 스키마를 일치시키려 SQL 기본값을 명시한다.
    @ColumnInfo(defaultValue = "1") val allDay: Boolean = true, // 종일(true)/특정 시각(false)
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    // --- 동기화 메타 ---
    val remoteId: String? = null,
    val syncStatus: String = SyncStatus.LOCAL_ONLY,
    val deletedAt: Long? = null,
)
