package com.kangtaeyoung.daynote.data.sync

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.kangtaeyoung.daynote.core.nowMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Android 동기화 매니저 — **인증만 담당**하고, push/공유 캘린더 pull 본체는
 * 플랫폼 공용 [GoogleCalendarSyncCore] 에 위임한다.
 *
 * - 로그인/인가: `rememberGoogleCalendarSignIn` 이 받은 액세스 토큰을 [onAccessToken] 으로 전달.
 *   **무음 재로그인**: 한 번 동의한 계정은 [silentAccessToken] 으로 재시작·토큰 만료 후에도
 *   동의 화면 없이 토큰을 자동 확보한다 → 세션마다 로그인할 필요가 없다.
 */
class AndroidCalendarSyncManager(
    private val context: Context,
    private val core: GoogleCalendarSyncCore,
) : CalendarSyncManager {

    override val isAvailable: Boolean = true

    private val _state = MutableStateFlow<SyncState>(SyncState.NeedsSetup)
    override val state: StateFlow<SyncState> = _state.asStateFlow()

    @Volatile
    var accessToken: String? = null
        private set

    fun onAccessToken(token: String?) {
        if (token.isNullOrBlank()) {
            _state.value = SyncState.Error("액세스 토큰을 받지 못했습니다.")
            return
        }
        accessToken = token
        signedOut = false // 명시적 로그인 — 이후 무음 재인가 허용.
        _state.value = SyncState.SignedIn("구글 계정")
    }

    fun onAuthError(message: String) {
        _state.value = SyncState.Error(message)
    }

    override suspend fun signOut() {
        accessToken = null
        signedOut = true // 명시적 로그아웃 — 무음 재인가도 막는다(다시 로그인 버튼을 누를 때까지).
        _state.value = SyncState.SignedOut
    }

    /** 사용자가 명시적으로 로그아웃했는지. true 면 무음 재인가를 시도하지 않는다. */
    @Volatile
    private var signedOut = false

    /**
     * 무음(silent) 재인가 — 이미 동의한 계정이면 동의 화면 없이 토큰을 받는다.
     * 동의가 필요한 첫 사용(hasResolution)이나 실패 시 null → 설정 화면에서 1회 로그인 필요.
     */
    private suspend fun silentAccessToken(): String? = suspendCancellableCoroutine { cont ->
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(GoogleAuthConfig.SCOPES.map(::Scope))
            .build()
        Identity.getAuthorizationClient(context)
            .authorize(request)
            .addOnSuccessListener { result ->
                cont.resume(if (result.hasResolution()) null else result.accessToken)
            }
            .addOnFailureListener { cont.resume(null) }
    }

    /** 현재 토큰이 없으면 무음 재인가로 확보한다(명시적 로그아웃 상태면 건너뜀). */
    private suspend fun ensureToken(): String? {
        accessToken?.let { return it }
        if (signedOut) return null
        val token = silentAccessToken() ?: return null
        accessToken = token
        _state.value = SyncState.SignedIn("구글 계정")
        return token
    }

    override suspend fun syncNow() {
        val token = ensureToken()
        if (token == null) {
            _state.value = SyncState.Error("먼저 구글 로그인이 필요합니다. (최초 1회, 설정 화면)")
            return
        }
        _state.value = SyncState.Syncing
        try {
            core.sync(token)
            _state.value = SyncState.Synced(nowMillis())
        } catch (e: CalendarApiException) {
            if (e.code == 401) {
                // 토큰 만료 — 무음 재인가로 새 토큰을 받아 1회 재시도(재로그인 요구하지 않음).
                accessToken = null
                val renewed = if (signedOut) null else silentAccessToken()
                if (renewed != null) {
                    accessToken = renewed
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
        val token = ensureToken()
            ?: return Result.failure(IllegalStateException("먼저 구글 로그인이 필요합니다."))
        return runCatching {
            core.listCalendars(token)
        }.recoverCatching { e ->
            // 403 = calendarlist 범위 미동의(범위 추가 전에 로그인한 계정) → 재로그인 안내.
            if (e is CalendarApiException && e.code == 403) {
                error("캘린더 목록 권한이 없습니다. '구글 로그인'을 다시 눌러 동의해 주세요.")
            }
            throw e
        }
    }
}
