package com.kangtaeyoung.daynote.data.backup

import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity
import kotlinx.serialization.Serializable

/**
 * 로컬 백업 파일 포맷(JSON). 메모·할 일 전체를 그대로 담아 기기 이전·복구에 쓴다.
 * Room 엔티티에 직접 @Serializable 을 붙이지 않고 DTO 로 분리(계층 격리).
 */
@Serializable
data class BackupFile(
    // 기본값 없음(필수) — "format" 이 없는 JSON 은 복원 시 즉시 거부되도록.
    val format: String,
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val notes: List<BackupNote> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
) {
    companion object {
        const val FORMAT = "daynote-backup"
    }
}

@Serializable
data class BackupNote(
    val id: String,
    val title: String,
    val content: String,
    val date: Long? = null,
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val syncStatus: String,
    val deletedAt: Long? = null,
)

@Serializable
data class BackupTask(
    val id: String,
    val noteId: String? = null,
    val text: String,
    val isDone: Boolean = false,
    val dueDate: Long? = null,
    val allDay: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val remoteId: String? = null,
    val syncStatus: String,
    val deletedAt: Long? = null,
    // 기간 할 일 종료일(v5) — 기본 null 이라 이전 백업 파일도 그대로 복원된다.
    val endDate: Long? = null,
)

fun NoteEntity.toBackup() = BackupNote(
    id, title, content, date, isPinned, createdAt, updatedAt, remoteId, syncStatus, deletedAt,
)

fun BackupNote.toEntity() = NoteEntity(
    id, title, content, date, isPinned, createdAt, updatedAt, remoteId, syncStatus, deletedAt,
)

fun TaskEntity.toBackup() = BackupTask(
    id, noteId, text, isDone, dueDate, allDay, sortOrder, createdAt, updatedAt, remoteId, syncStatus, deletedAt, endDate,
)

fun BackupTask.toEntity() = TaskEntity(
    id, noteId, text, isDone, dueDate, allDay, sortOrder, createdAt, updatedAt, remoteId, syncStatus, deletedAt, endDate,
)
