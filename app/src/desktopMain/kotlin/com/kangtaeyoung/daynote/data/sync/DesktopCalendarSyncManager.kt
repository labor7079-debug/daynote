package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.data.security.SecureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Desktop 동기화 매니저 — [DesktopGoogleAuth](PKCE 루프백)로 토큰을 확보하고,
 * push/공유 캘린더 pull 은 플랫폼 공용 [GoogleCalendarSyncCore] 에 위임한다.
 *
 * - 사용 조건: 빌드에 데스크톱 OAuth 클라이언트가 주입돼 있어야 한다
 *   (keystore.properties 의 googleDesktopClientId/Secret → 없으면 [isAvailable]=false).
 * - 로그인: 설정의 "구글 로그인" 버튼 → 시스템 브라우저 동의 → 자동 복귀.
 *   이후에는 리프레시 토큰으로 재시작해도 무음 갱신된다.
 */
class DesktopCalendarSyncManager(
    secure: SecureStore,
    private val core: GoogleCalendarSyncCore,
) : CalendarSyncManager {

    private val auth = DesktopGoogleAuth(secure)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val isAvailable: Boolean = auth.isConfigured

    private val _state = MutableStateFlow<SyncState>(
        when {
            !auth.isConfigured -> SyncState.NeedsSetup
            auth.hasSession() -> SyncState.SignedIn("구글 계정")
            else -> SyncState.SignedOut
        },
    )
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    @Volatile
    private var signingIn = false

    /** 브라우저 동의 플로우 시작(설정의 "구글 로그인" 버튼). 진행 중이면 무시. */
    fun startSignIn() {
        if (!isAvailable || signingIn) return
        signingIn = true
        scope.launch {
            runCatching { auth.signIn() }
                .onSuccess { _state.value = SyncState.SignedIn("구글 계정") }
                .onFailure { _state.value = SyncState.Error(it.message ?: "구글 로그인 실패") }
            signingIn = false
        }
    }

    override suspend fun signOut() {
        auth.clear()
        _state.value = if (isAvailable) SyncState.SignedOut else SyncState.NeedsSetup
    }

    override suspend fun syncNow() {
        if (!isAvailable) {
            _state.value = SyncState.NeedsSetup
            return
        }
        val token = auth.ensureAccessToken()
        if (token == null) {
            _state.value = SyncState.Error("먼저 구글 로그인이 필요합니다. (설정 화면, 브라우저 1회)")
            return
        }
        _state.value = SyncState.Syncing
        try {
            core.sync(token)
            _state.value = SyncState.Synced(nowMillis())
        } catch (e: CalendarApiException) {
            if (e.code == 401) {
                // 액세스 토큰 만료/폐기 — 리프레시로 강제 갱신 후 1회 재시도.
                val renewed = auth.ensureAccessToken(forceRefresh = true)
                if (renewed != null) {
                    runCatching { core.sync(renewed) }
                        .onSuccess { _state.value = SyncState.Synced(nowMillis()) }
                        .onFailure { _state.value = SyncState.Error(it.message ?: "동기화 실패") }
                } else {
                    _state.value = SyncState.Error("세션이 만료됐습니다. 다시 로그인해 주세요.")
                }
            } else {
                _state.value = SyncState.Error(e.message ?: "동기화 실패")
            }
        } catch (e: Exception) {
            _state.value = SyncState.Error(e.message ?: "동기화 실패")
        }
    }

    override suspend fun listCalendars(): Result<List<GoogleCalendarInfo>> {
        if (!isAvailable) return Result.failure(IllegalStateException("데스크톱 OAuth 클라이언트가 설정되지 않았습니다."))
        val token = auth.ensureAccessToken()
            ?: return Result.failure(IllegalStateException("먼저 구글 로그인이 필요합니다."))
        return runCatching {
            core.listCalendars(token)
        }.recoverCatching { e ->
            if (e is CalendarApiException && e.code == 403) {
                error("캘린더 목록 권한이 없습니다. '구글 로그인'을 다시 눌러 동의해 주세요.")
            }
            throw e
        }
    }
}
