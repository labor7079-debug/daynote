package com.kangtaeyoung.daynote.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.data.backup.BackupManager
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.security.ApiKeyProvider
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncState
import com.kangtaeyoung.daynote.data.sync.GoogleCalendarInfo
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
    private val backup: BackupManager,
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

    // --- 표시할 구글 캘린더(공유받은 캘린더 포함) ---
    private val _googleCalendars = MutableStateFlow<List<GoogleCalendarInfo>>(emptyList())
    /** 불러온 캘린더 목록(체크 UI 용). "캘린더 목록 불러오기"를 눌러야 채워진다. */
    val googleCalendars: StateFlow<List<GoogleCalendarInfo>> = _googleCalendars.asStateFlow()

    private val _googleCalendarsMsg = MutableStateFlow<String?>(null)
    val googleCalendarsMsg: StateFlow<String?> = _googleCalendarsMsg.asStateFlow()

    private val _googleCalendarsLoading = MutableStateFlow(false)
    val googleCalendarsLoading: StateFlow<Boolean> = _googleCalendarsLoading.asStateFlow()

    /** 체크된(달력에 표시할) 캘린더 id 집합 — 영속. */
    val visibleCalendarIds: StateFlow<Set<String>> = settings.observeVisibleGoogleCalendarIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    fun loadGoogleCalendars() {
        if (_googleCalendarsLoading.value) return
        _googleCalendarsLoading.value = true
        _googleCalendarsMsg.value = null
        viewModelScope.launch {
            sync.listCalendars()
                .onSuccess { list ->
                    _googleCalendars.value = list.sortedWith(compareByDescending<GoogleCalendarInfo> { it.primary }.thenBy { it.name })
                    if (list.isEmpty()) _googleCalendarsMsg.value = "캘린더가 없습니다."
                }
                .onFailure { _googleCalendarsMsg.value = it.message ?: "캘린더 목록을 불러오지 못했습니다." }
            _googleCalendarsLoading.value = false
        }
    }

    /** 체크 변경 즉시 영속 + 동기화로 pull 반영(해제 시 캐시 정리도 pull 이 담당). */
    fun setCalendarVisible(id: String, visible: Boolean) {
        viewModelScope.launch {
            val current = settings.getVisibleGoogleCalendarIds().toMutableSet()
            if (visible) current += id else current -= id
            settings.setVisibleGoogleCalendarIds(current)
            sync.syncNow()
        }
    }

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

    // --- AI 제목 자동생성 토글(저장 시 제목 비면 자동 생성, 기본 켜짐) ---
    val autoTitle: StateFlow<Boolean> = settings.observeAutoTitle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setAutoTitle(enabled: Boolean) {
        viewModelScope.launch { settings.setAutoTitle(enabled) }
    }

    // --- 할 일 마감 알림 토글(기본 켜짐) ---
    val remindersEnabled: StateFlow<Boolean> = settings.observeRemindersEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setRemindersEnabled(enabled) }
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

    // --- 로컬 백업/복원 ---
    private val _backupMsg = MutableStateFlow<String?>(null)
    val backupMsg: StateFlow<String?> = _backupMsg.asStateFlow()

    fun setBackupMsg(msg: String) { _backupMsg.value = msg }
    fun clearBackupMsg() { _backupMsg.value = null }

    /** 내보내기: DB 를 JSON 으로 만든 뒤 [onReady] 로 넘겨 파일 저장(플랫폼 IO)을 시작한다. */
    fun buildExport(onReady: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { backup.exportJson() }
                .onSuccess { onReady(it) }
                .onFailure { _backupMsg.value = "내보내기 준비 실패: ${it.message}" }
        }
    }

    /** 가져오기: 선택한 파일 내용을 복원(upsert). */
    fun importBackup(json: String) {
        viewModelScope.launch {
            runCatching { backup.importJson(json) }
                .onSuccess { _backupMsg.value = "복원 완료 — 메모 ${it.notes}건 · 할 일 ${it.tasks}건" }
                .onFailure { _backupMsg.value = "복원 실패: 올바른 DayNote 백업 파일인지 확인하세요." }
        }
    }
}
