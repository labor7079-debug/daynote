package com.kangtaeyoung.daynote.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Desktop 동기화 매니저 — 비활성 스텁. 구글 캘린더 연동은 Android 전용이라 데스크톱은 미지원.
 * (필요해지면 데스크톱용 OAuth 데스크톱 플로우를 별도로 검토.)
 */
class DesktopCalendarSyncManager : CalendarSyncManager {

    override val isAvailable: Boolean = false

    private val _state = MutableStateFlow<SyncState>(SyncState.Unavailable)
    override val state: StateFlow<SyncState> = _state

    override suspend fun signOut() {
        _state.value = SyncState.Unavailable
    }

    override suspend fun syncNow() {
        _state.value = SyncState.Unavailable
    }
}
