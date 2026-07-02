package com.kangtaeyoung.daynote.data.sync.supabase

import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import com.kangtaeyoung.daynote.data.local.entity.SyncStatus
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity

/**
 * 엔티티 ↔ Supabase 행 매핑. 클라우드 행은 캘린더 전용 메타(remoteId/syncStatus)를 모른다 —
 * pull 로 로컬에 반영할 때는 기존 로컬 행의 그 메타를 보존한다.
 */

fun NoteEntity.toRow(userId: String): NoteRow = NoteRow(
    id = id,
    userId = userId,
    title = title,
    content = content,
    date = date,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** [existing] = 로컬 현재 행(없으면 신규). 캘린더 메타는 보존한다. */
fun NoteRow.toEntity(existing: NoteEntity?): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    date = date,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = existing?.remoteId,
    syncStatus = existing?.syncStatus ?: SyncStatus.SYNCED,
    deletedAt = deletedAt,
)

fun TaskEntity.toRow(userId: String): TaskRow = TaskRow(
    id = id,
    userId = userId,
    noteId = noteId,
    text = text,
    isDone = isDone,
    dueDate = dueDate,
    allDay = allDay,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun TaskRow.toEntity(existing: TaskEntity?): TaskEntity = TaskEntity(
    id = id,
    noteId = noteId,
    text = text,
    isDone = isDone,
    dueDate = dueDate,
    allDay = allDay,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    remoteId = existing?.remoteId,
    syncStatus = existing?.syncStatus ?: SyncStatus.SYNCED,
    deletedAt = deletedAt,
    // endDate 는 아직 서버 컬럼이 없어 push 하지 않는다 — pull 이 로컬 값을 지우지 않게 보존.
    // (멀티기기에서 기간을 동기화하려면 Supabase tasks 에 end_date 컬럼 추가 후 TaskRow 에 반영.)
    endDate = existing?.endDate,
)
