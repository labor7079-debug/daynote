package com.kangtaeyoung.daynote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.SyncState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 설정 화면 — 구글 캘린더 동기화. 토글 상태는 [SettingsRepository] 로 영속화(재시작 후 유지). */
class SettingsViewModel(
    private val sync: CalendarSyncManager,
    private val settings: SettingsRepository,
) : ViewModel() {

    val syncAvailable: Boolean = sync.isAvailable
    val state: StateFlow<SyncState> = sync.state

    val syncEnabled: StateFlow<Boolean> = settings.observeSyncEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setSyncEnabled(enabled)
            if (!enabled) sync.signOut() // 끄면 토큰 정리(로그아웃)
        }
    }

    fun signOut() {
        viewModelScope.launch { sync.signOut() }
    }

    fun syncNow() {
        viewModelScope.launch { sync.syncNow() }
    }
}
