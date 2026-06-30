package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import com.kangtaeyoung.daynote.data.local.entity.SyncStatus
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.model.Task

/**
 * 엔티티 ↔ 도메인 매핑. 동기화 메타는 데이터 계층에만 머무른다.
 *
 * Phase 1 은 로컬 전용이라 저장 시 syncStatus 는 항상 LOCAL_ONLY, remoteId/deletedAt 은 기본값이다.
 * (Phase 6 동기화 도입 시 이 매핑/Repository 에 상태 전이 규칙을 넣는다.)
 */

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    date = date,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Note.toEntity(updatedAt: Long = this.updatedAt): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    date = date,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = null,
    syncStatus = SyncStatus.LOCAL_ONLY,
    deletedAt = null,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    noteId = noteId,
    text = text,
    isDone = isDone,
    dueDate = dueDate,
    allDay = allDay,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Task.toEntity(updatedAt: Long = this.updatedAt): TaskEntity = TaskEntity(
    id = id,
    noteId = noteId,
    text = text,
    isDone = isDone,
    dueDate = dueDate,
    allDay = allDay,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = null,
    syncStatus = SyncStatus.LOCAL_ONLY,
    deletedAt = null,
)
