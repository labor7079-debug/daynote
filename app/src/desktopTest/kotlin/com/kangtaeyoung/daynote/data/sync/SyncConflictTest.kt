package com.kangtaeyoung.daynote.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals

/** Phase 6-A — last-write-wins 충돌 해소(순수 함수) 검증. */
class SyncConflictTest {

    @Test
    fun remoteNewer_wins() {
        assertEquals(ConflictWinner.REMOTE, resolveByUpdatedAt(localUpdatedAt = 100, remoteUpdatedAt = 200))
    }

    @Test
    fun localNewer_wins() {
        assertEquals(ConflictWinner.LOCAL, resolveByUpdatedAt(localUpdatedAt = 300, remoteUpdatedAt = 200))
    }

    @Test
    fun tie_keepsLocal() {
        // 같으면 불필요한 덮어쓰기를 피해 로컬 유지.
        assertEquals(ConflictWinner.LOCAL, resolveByUpdatedAt(localUpdatedAt = 150, remoteUpdatedAt = 150))
    }

    @Test
    fun supabaseConfig_validity() {
        assertEquals(false, SupabaseConfig.EMPTY.isValid)
        assertEquals(false, SupabaseConfig(url = "https://x.supabase.co", anonKey = "").isValid)
        assertEquals(true, SupabaseConfig(url = "https://x.supabase.co", anonKey = "k").isValid)
    }
}
