package com.kangtaeyoung.daynote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.security.ApiKeyProvider
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncState
import com.kangtaeyoung.daynote.data.sync.SupabaseConfig
import com.kangtaeyoung.daynote.data.sync.SyncState
import com.kangtaeyoung.daynote.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 설정 화면 — 구글 캘린더 + 클라우드(Supabase) 동기화 + AI(OpenAI) 키. 상태는 [SettingsRepository] 로 영속화. */
class SettingsViewModel(
    private val sync: CalendarSyncManager,
    private val settings: SettingsRepository,
    private val apiKeys: ApiKeyProvider,
    private val cloud: CloudSyncManager,
) : ViewModel() {

    val syncAvailable: Boolean = sync.isAvailable
    val state: StateFlow<SyncState> = sync.state

    // --- 화면 테마 ---
    val themeMode: StateFlow<ThemeMode> = settings.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    val syncEnabled: StateFlow<Boolean> = settings.observeSyncEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // --- 클라우드(Supabase) 동기화 ---
    val cloudState: StateFlow<CloudSyncState> = cloud.state

    val cloudSyncEnabled: StateFlow<Boolean> = settings.observeCloudSyncEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val supabaseConfig: StateFlow<SupabaseConfig> = settings.observeSupabaseConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SupabaseConfig.EMPTY)

    /** 로그인/회원가입 진행 표시. */
    private val _cloudBusy = MutableStateFlow(false)
    val cloudBusy: StateFlow<Boolean> = _cloudBusy.asStateFlow()

    init {
        viewModelScope.launch { cloud.refreshState() }
    }

    fun setCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setCloudSyncEnabled(enabled)
            // refreshState/syncNow 가 토글 상태를 읽어 켜짐=동기화 / 꺼짐=Disabled 로 상태를 정리한다.
            if (enabled) cloud.syncNow() else cloud.refreshState()
        }
    }

    fun saveSupabaseConfig(url: String, anonKey: String) {
        viewModelScope.launch {
            settings.setSupabaseConfig(SupabaseConfig(url, anonKey))
            cloud.refreshState()
        }
    }

    fun cloudSignIn(email: String, password: String) {
        if (_cloudBusy.value) return
        _cloudBusy.value = true
        viewModelScope.launch {
            cloud.signIn(email, password)
            _cloudBusy.value = false
        }
    }

    fun cloudSignUp(email: String, password: String) {
        if (_cloudBusy.value) return
        _cloudBusy.value = true
        viewModelScope.launch {
            cloud.signUp(email, password)
            _cloudBusy.value = false
        }
    }

    fun cloudSignOut() {
        viewModelScope.launch { cloud.signOut() }
    }

    fun cloudSyncNow() {
        viewModelScope.launch { cloud.syncNow() }
    }

    private val _hasApiKey = MutableStateFlow(apiKeys.hasKey())
    /** OpenAI 키가 저장돼 있는지(원문은 노출하지 않는다). */
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    fun saveApiKey(key: String) {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return
        apiKeys.setOpenAiKey(trimmed)
        _hasApiKey.value = true
    }

    fun clearApiKey() {
        apiKeys.clear()
        _hasApiKey.value = false
    }

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
