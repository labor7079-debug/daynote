package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import com.kangtaeyoung.daynote.data.local.entity.SyncStatus
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity
import com.kangtaeyoung.daynote.data.sync.supabase.toEntity
import com.kangtaeyoung.daynote.data.sync.supabase.toRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Phase 6-B — 엔티티 ↔ Supabase 행 매핑 검증(네트워크 없이). */
class SupabaseMappingTest {

    @Test
    fun note_roundTrip_andPullPreservesCalendarMeta() {
        val local = NoteEntity(
            id = "n1", title = "제목", content = "본문", date = 123L, isPinned = true,
            createdAt = 10L, updatedAt = 20L,
            remoteId = "gcal-event-9", syncStatus = SyncStatus.SYNCED, deletedAt = null,
        )

        // push: 행으로 변환 — userId 주입, 캘린더 메타(remoteId)는 행에 없음.
        val row = local.toRow(userId = "user-1")
        assertEquals("user-1", row.userId)
        assertEquals("본문", row.content)
        assertEquals(20L, row.updatedAt)

        // pull: 기존 로컬을 넘기면 캘린더 메타(remoteId/syncStatus)는 보존된다.
        val applied = row.toEntity(existing = local)
        assertEquals("gcal-event-9", applied.remoteId)
        assertEquals(SyncStatus.SYNCED, applied.syncStatus)
        assertEquals("본문", applied.content)
    }

    @Test
    fun note_pullNew_hasNoRemoteId_andSyncedStatus() {
        val row = NoteEntity(id = "n2", title = "t", content = "c", createdAt = 1L, updatedAt = 2L)
            .toRow("user-1")
        val applied = row.toEntity(existing = null) // 다른 기기가 만든 신규
        assertNull(applied.remoteId)
        assertEquals(SyncStatus.SYNCED, applied.syncStatus)
    }

    @Test
    fun task_softDelete_propagatesViaRow() {
        val deleted = TaskEntity(
            id = "t1", text = "할일", createdAt = 1L, updatedAt = 50L, deletedAt = 50L,
        )
        val row = deleted.toRow("user-1")
        assertEquals(50L, row.deletedAt) // tombstone 이 행에 실려 다른 기기로 전파됨
        assertEquals(50L, row.toEntity(existing = deleted).deletedAt)
    }
}
